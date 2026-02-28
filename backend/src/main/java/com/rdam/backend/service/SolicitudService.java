package com.rdam.backend.service;
import com.rdam.backend.domain.dto.CrearSolicitudRequest;
import com.rdam.backend.domain.dto.CrearSolicitudResponse;
import com.rdam.backend.domain.dto.SolicitudEstadoResponse;
import com.rdam.backend.domain.dto.ValidarOtpResponse;
import com.rdam.backend.exception.EstadoInvalidoException;
import com.rdam.backend.exception.SolicitudNotFoundException;
import com.rdam.backend.exception.TokenInvalidoException;
import com.rdam.backend.domain.entity.Circunscripcion;
import com.rdam.backend.domain.entity.Solicitud;
import com.rdam.backend.domain.entity.SolicitudHistorialEstado;
import com.rdam.backend.domain.entity.UsuarioInterno;
import com.rdam.backend.enums.EstadoSolicitud;
import com.rdam.backend.enums.RolUsuario;
import com.rdam.backend.repository.CircunscripcionRepository;
import com.rdam.backend.repository.SolicitudHistorialEstadoRepository;
import com.rdam.backend.repository.SolicitudRepository;
import com.rdam.backend.service.TokenService.ResultadoValidacionOtp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orquestador central del sistema rdam.backend.
 *
 * Implementa el ciclo de vida completo de una solicitud:
 *   Crear → Validar OTP → Pagar → Publicar → Vencer
 *
 * Responsabilidades:
 *   - Crear solicitudes y generar nroTramite
 *   - Validar OTP y emitir token de acceso ciudadano
 *   - Procesar webhooks de pago de PlusPagos
 *   - Coordinar la carga de certificados con CertificadoService
 *   - Filtrar solicitudes según el rol y circunscripción del usuario
 *   - Registrar cada cambio de estado en el historial
 *
 * No implementa lógica de negocio directamente:
 *   - Las transiciones de estado las valida SolicitudStateMachine
 *   - Los tokens los gestiona TokenService
 *   - Los emails los envía EmailService
 *   - Los archivos los maneja CertificadoService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudService {

    private final SolicitudRepository            solicitudRepository;
    private final CircunscripcionRepository      circunscripcionRepository;
    private final SolicitudHistorialEstadoRepository historialRepository;
    private final SolicitudStateMachine          stateMachine;
    private final TokenService                   tokenService;
    private final EmailService                   emailService;
    private final PagoService                    pagoService;
    private final CertificadoService             certificadoService;

    @Value("${rdam.backend.negocio.monto-arancel}")
    private BigDecimal montoArancel;

    @Value("${rdam.backend.negocio.validez-certificado-dias}")
    private int validezCertificadoDias;

    @Value("${server.base-url:http://localhost:8080}")
    private String baseUrl;

    // Contador en memoria para el secuencial del nroTramite.
    // En producción con múltiples instancias esto debería
    // estar en Redis o usar una secuencia de DB.
    private final AtomicInteger contadorTramite = new AtomicInteger(0);

    // =========================================================
    // PORTAL CIUDADANO
    // =========================================================

    /**
     * Crea una nueva solicitud de certificado.
     *
     * Flujo:
     *   1. Valida que la circunscripción exista
     *   2. Genera el nroTramite único
     *   3. Persiste la solicitud en estado PENDIENTE
     *   4. Genera y guarda el OTP en Redis (TTL 15 min)
     *   5. Envía el OTP por email (async)
     *
     * @Transactional: si algo falla, el INSERT de la solicitud
     * se revierte. El email es async y no forma parte de la TX.
     */
    @Transactional
    public CrearSolicitudResponse crearSolicitud(CrearSolicitudRequest request) {

        // Verificar que la circunscripción existe
        Circunscripcion circunscripcion = circunscripcionRepository
            .findById(request.getIdCircunscripcion())
            .orElseThrow(() -> new IllegalArgumentException(
                "Circunscripción no encontrada: " + request.getIdCircunscripcion()
            ));

        // Construir y persistir la solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setDniCuil(request.getDniCuil());
        solicitud.setEmail(request.getEmail());
        solicitud.setCircunscripcion(circunscripcion);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setNroTramite(generarNroTramite());

        Solicitud guardada = solicitudRepository.save(solicitud);

        // Registrar en historial (estado inicial no tiene estado anterior)
        registrarHistorial(guardada, null, EstadoSolicitud.PENDIENTE, null);

        // Generar OTP y guardarlo en Redis
        String otp = tokenService.generarYGuardarOtp(guardada.getId());

        // Enviar email de forma asíncrona (no bloquea esta transacción)
        emailService.enviarOtp(
            guardada.getEmail(),
            guardada.getNroTramite(),
            otp
        );

        log.info("Solicitud creada. id={} nroTramite={} circunscripcion={}",
                guardada.getId(),
                guardada.getNroTramite(),
                circunscripcion.getNombre());

        return new CrearSolicitudResponse(
            guardada.getId(),
            guardada.getNroTramite(),
            "Te enviamos un código de verificación a " + request.getEmail()
        );
    }

    /**
     * Valida el OTP ingresado por el ciudadano.
     *
     * Flujo:
     *   1. Verifica que la solicitud exista
     *   2. Valida el OTP contra Redis (máx 3 intentos, TTL 15 min)
     *   3. Genera el token de acceso ciudadano en Redis
     *   4. Guarda referencia del token en la solicitud
     *   5. Limpia el código OTP de la solicitud
     *
     * @param idSolicitud ID de la solicitud (viene en la URL)
     * @param codigo      OTP de 6 dígitos ingresado por el ciudadano
     */
    @Transactional
    public ValidarOtpResponse validarOtp(Long idSolicitud, String codigo) {

        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
            .orElseThrow(() -> new SolicitudNotFoundException(
                String.valueOf(idSolicitud)
            ));

        // Validar OTP contra Redis
        ResultadoValidacionOtp resultado =
            tokenService.validarOtp(idSolicitud, codigo);

        // Manejar cada resultado posible
        switch (resultado) {
            case EXPIRADO -> throw new TokenInvalidoException(
                "El código de verificación expiró. " +
                "Por favor iniciá una nueva solicitud."
            );
            case INTENTOS_AGOTADOS -> throw new TokenInvalidoException(
                "Superaste el límite de intentos. " +
                "El código fue invalidado."
            );
            case INCORRECTO -> throw new TokenInvalidoException(
                "Código incorrecto. Verificá el email e intentá nuevamente."
            );
            case VALIDO -> { /* continúa el flujo */ }
        }

        // OTP válido: generar token de acceso ciudadano
        String tokenAcceso = tokenService.generarTokenCiudadano(
            solicitud.getNroTramite()
        );

        // Guardar referencia del token en la solicitud
        solicitud.setTokenAcceso(tokenAcceso);
        solicitud.setCodigoValidacion(null); // limpiar OTP por seguridad
        solicitudRepository.save(solicitud);

        log.info("OTP validado exitosamente. id={} nroTramite={}",
                idSolicitud, solicitud.getNroTramite());

        return new ValidarOtpResponse(
            tokenAcceso,
            solicitud.getNroTramite(),
            solicitud.getEstado().name()
        );
    }

    /**
     * Consulta el estado de una solicitud para el ciudadano.
     *
     * Solo expone campos visibles al ciudadano.
     * El linkDescarga solo aparece cuando el estado es PUBLICADO.
     *
     * @param nroTramite Número de trámite público.
     */
    @Transactional(readOnly = true)
    public SolicitudEstadoResponse consultarEstado(String nroTramite) {
        Solicitud solicitud = solicitudRepository
            .findByNroTramite(nroTramite)
            .orElseThrow(() -> new SolicitudNotFoundException(nroTramite));

        return new SolicitudEstadoResponse(solicitud, baseUrl);
    }

    /**
     * Crea una orden de pago para una solicitud.
     *
     * Valida que la solicitud esté en estado PENDIENTE antes
     * de generar la orden. En modo simulación devuelve una
     * URL mock para que el desarrollador pruebe el flujo.
     *
     * @param idSolicitud ID de la solicitud.
     * @return Resultado con urlPago e idOrdenPago.
     */
    @Transactional
    public PagoService.ResultadoOrdenPago crearOrdenPago(Long idSolicitud) {

        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
            .orElseThrow(() -> new SolicitudNotFoundException(
                String.valueOf(idSolicitud)
            ));

        // Solo se puede pagar una solicitud PENDIENTE
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new EstadoInvalidoException(
                "La solicitud debe estar en estado PENDIENTE para iniciar el pago. " +
                "Estado actual: " + solicitud.getEstado()
            );
        }

        // Convertir el monto a centavos para PlusPagos
        long montoCentavos = montoArancel
            .multiply(BigDecimal.valueOf(100))
            .longValue();

        return pagoService.crearOrdenPago(
            idSolicitud,
            solicitud.getNroTramite(),
            montoCentavos
        );
    }

    // =========================================================
    // WEBHOOK DE PAGOS
    // =========================================================

    /**
     * Procesa el resultado de un pago recibido por webhook de PlusPagos.
     *
     * Idempotencia: si el mismo idOrdenPago llega dos veces,
     * el segundo se ignora silenciosamente.
     *
     * Flujo para pago APROBADO:
     *   1. Busca la solicitud por idOrdenPago
     *   2. Valida idempotencia
     *   3. Transiciona a PAGADO
     *   4. Notifica al ciudadano por email
     *
     * Flujo para pago RECHAZADO:
     *   1. Busca la solicitud por idOrdenPago
     *   2. Transiciona a VENCIDO
     *   3. Notifica al ciudadano por email
     *
     * @param idOrdenPago   ID de la orden en PlusPagos.
     * @param codigoEstado  Código de resultado (0=aprobado, 4/7/8/9/11=rechazado).
     * @param monto         Monto confirmado por PlusPagos.
     */
    @Transactional
    public void procesarWebhookPago(String idOrdenPago,
                                     int codigoEstado,
                                     BigDecimal monto) {

        Solicitud solicitud = solicitudRepository
            .findByIdOrdenPago(idOrdenPago)
            .orElseThrow(() -> new SolicitudNotFoundException(
                "idOrdenPago=" + idOrdenPago
            ));

        // Idempotencia: si ya fue procesado, ignorar silenciosamente
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            log.warn("Webhook duplicado ignorado. idOrdenPago={} estadoActual={}",
                    idOrdenPago, solicitud.getEstado());
            return;
        }

        PagoService.ResultadoPago resultado =
            pagoService.interpretarCodigoEstado(codigoEstado);

        if (resultado == PagoService.ResultadoPago.APROBADO) {
            // Validar transición antes de modificar
            stateMachine.validarTransicion(
                solicitud.getEstado(), EstadoSolicitud.PAGADO
            );

            solicitud.setEstado(EstadoSolicitud.PAGADO);
            solicitud.setIdOrdenPago(idOrdenPago);
            solicitud.setMontoArancel(monto);
            solicitud.setSolFecPago(LocalDateTime.now());
            solicitudRepository.save(solicitud);

            registrarHistorial(
                solicitud,
                EstadoSolicitud.PENDIENTE,
                EstadoSolicitud.PAGADO,
                null // cambio automático, sin usuario interno
            );

            emailService.notificarPagoConfirmado(
                solicitud.getEmail(),
                solicitud.getNroTramite()
            );

            log.info("Pago aprobado procesado. nroTramite={} monto={}",
                    solicitud.getNroTramite(), monto);

        } else {
            stateMachine.validarTransicion(
                solicitud.getEstado(), EstadoSolicitud.VENCIDO
            );

            solicitud.setEstado(EstadoSolicitud.VENCIDO);
            solicitudRepository.save(solicitud);

            registrarHistorial(
                solicitud,
                EstadoSolicitud.PENDIENTE,
                EstadoSolicitud.VENCIDO,
                null
            );

            emailService.notificarSolicitudVencida(
                solicitud.getEmail(),
                solicitud.getNroTramite()
            );

            log.info("Pago rechazado procesado. nroTramite={} codigo={}",
                    solicitud.getNroTramite(), codigoEstado);
        }
    }

    // =========================================================
    // PANEL INTERNO — OPERADOR / ADMIN
    // =========================================================

    /**
     * Lista solicitudes filtradas según el rol del usuario.
     *
     * Si el usuario es OPERADOR: solo ve las de su circunscripción.
     * Si el usuario es ADMIN: puede filtrar por cualquier circunscripción.
     *
     * El filtro automático por circunscripción es la regla de
     * negocio más importante del panel interno.
     *
     * @param usuario             Usuario autenticado (del JWT).
     * @param idCircunscripcion   Filtro opcional (solo ADMIN puede usarlo).
     * @param estado              Filtro opcional por estado.
     * @param dniCuil             Filtro opcional por DNI/CUIL.
     * @param pageable            Paginación y ordenamiento.
     */
    @Transactional(readOnly = true)
    public Page<Solicitud> listarSolicitudes(UsuarioInterno usuario,
                                              Integer idCircunscripcion,
                                              EstadoSolicitud estado,
                                              String dniCuil,
                                              Pageable pageable) {
        if (usuario.getRol() == RolUsuario.OPERADOR) {
            // OPERADOR: ignora cualquier filtro de circunscripción
            // que venga del request. Siempre usa la suya.
            Integer idCircunscripcionOperador =
                usuario.getCircunscripcion().getId();

            return solicitudRepository.buscarPorCircunscripcion(
                idCircunscripcionOperador,
                estado,
                dniCuil,
                pageable
            );
        }

        // ADMIN: usa el filtro de circunscripción si viene,
        // o lista todas si no se especifica.
        return solicitudRepository.buscarConFiltros(
            idCircunscripcion,
            estado,
            dniCuil,
            pageable
        );
    }

    /**
     * Sube el certificado PDF para una solicitud en estado PAGADO.
     *
     * Flujo:
     *   1. Verifica que la solicitud esté en estado PAGADO
     *   2. Verifica que el operador tenga acceso a esa circunscripción
     *   3. Sube el PDF a MinIO
     *   4. Genera el token de descarga
     *   5. Transiciona a PUBLICADO
     *   6. Notifica al ciudadano con el link de descarga
     *
     * @param idSolicitud  ID de la solicitud.
     * @param archivo      Archivo PDF del certificado.
     * @param usuario      Usuario interno que realiza la operación.
     */
    @Transactional
    public String subirCertificado(Long idSolicitud,
                                    MultipartFile archivo,
                                    UsuarioInterno usuario) throws IOException {

        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
            .orElseThrow(() -> new SolicitudNotFoundException(
                String.valueOf(idSolicitud)
            ));

        // Verificar estado PAGADO
        if (solicitud.getEstado() != EstadoSolicitud.PAGADO) {
            throw new EstadoInvalidoException(
                "La solicitud debe estar en estado PAGADO para subir " +
                "el certificado. Estado actual: " + solicitud.getEstado()
            );
        }

        // Verificar acceso por circunscripción
        verificarAccesoCircunscripcion(usuario, solicitud);

        // Subir PDF a MinIO
        String rutaMinIO = certificadoService.subirCertificado(
            idSolicitud, archivo
        );

        // Generar token de descarga único de 64 chars
        String tokenDescarga = certificadoService.generarTokenDescarga();

        // Transicionar estado
        stateMachine.validarTransicion(
            solicitud.getEstado(), EstadoSolicitud.PUBLICADO
        );

        solicitud.setEstado(EstadoSolicitud.PUBLICADO);
        solicitud.setUrlCertificado(rutaMinIO);
        solicitud.setTokenDescarga(tokenDescarga);
        solicitud.setSolFecEmision(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        registrarHistorial(
            solicitud,
            EstadoSolicitud.PAGADO,
            EstadoSolicitud.PUBLICADO,
            usuario.getId()
        );

        // Construir URL pública de descarga
        String urlDescarga = baseUrl + "/api/v1/certificados/" + tokenDescarga;

        // Notificar al ciudadano (async)
        emailService.notificarCertificadoDisponible(
            solicitud.getEmail(),
            solicitud.getNroTramite(),
            urlDescarga,
            validezCertificadoDias
        );

        log.info("Certificado publicado. nroTramite={} operador={} circunscripcion={}",
                solicitud.getNroTramite(),
                usuario.getUsername(),
                solicitud.getCircunscripcion().getNombre());

        return tokenDescarga;
    }

    /**
     * Regenera el token de descarga sin reemplazar el archivo en MinIO.
     * Útil cuando el ciudadano perdió el email o el token venció.
     *
     * El token anterior queda inválido al sobreescribirse en DB.
     *
     * Solo válido para solicitudes en estado PUBLICADO o PUBLICADO_VENCIDO.
     */
    @Transactional
    public String regenerarTokenDescarga(Long idSolicitud,
                                          UsuarioInterno usuario) {

        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
            .orElseThrow(() -> new SolicitudNotFoundException(
                String.valueOf(idSolicitud)
            ));

        if (solicitud.getEstado() != EstadoSolicitud.PUBLICADO &&
            solicitud.getEstado() != EstadoSolicitud.PUBLICADO_VENCIDO) {
            throw new EstadoInvalidoException(
                "Solo se puede regenerar el token en estado PUBLICADO " +
                "o PUBLICADO_VENCIDO. Estado actual: " + solicitud.getEstado()
            );
        }

        verificarAccesoCircunscripcion(usuario, solicitud);

        String nuevoToken = certificadoService.generarTokenDescarga();
        solicitud.setTokenDescarga(nuevoToken);
        solicitud.setSolFecEmision(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        log.info("Token de descarga regenerado. nroTramite={} operador={}",
                solicitud.getNroTramite(), usuario.getUsername());

        return nuevoToken;
    }

    // =========================================================
    // Helpers privados
    // =========================================================

    /**
     * Genera el número de trámite con formato RDAM-YYYYMMDD-NNNN.
     *
     * Ejemplo: RDAM-20260215-0042
     *
     * El secuencial se resetea con cada reinicio de la app.
     * En producción con múltiples instancias, esto debería
     * manejarse con una secuencia de DB o Redis.
     */
    private String generarNroTramite() {
        String fecha = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int secuencial = contadorTramite.incrementAndGet();
        return String.format("RDAM-%s-%04d", fecha, secuencial);
    }

    /**
     * Verifica que el usuario tenga acceso a la circunscripción
     * de la solicitud. Si es ADMIN, siempre puede. Si es OPERADOR,
     * solo puede si la circunscripción coincide con la suya.
     *
     * @throws CircunscripcionMismatchException si el acceso está denegado.
     */
    private void verificarAccesoCircunscripcion(UsuarioInterno usuario,
                                                 Solicitud solicitud) {
        if (!usuario.puedeAccederCircunscripcion(
                solicitud.getCircunscripcion().getId())) {
            throw new com.rdam.backend.exception
                .CircunscripcionMismatchException();
        }
    }

    /**
     * Registra un cambio de estado en la tabla de historial.
     * Se llama cada vez que el estado de una solicitud cambia.
     *
     * @param solicitud       La solicitud modificada.
     * @param estadoAnterior  Estado previo (null si es el estado inicial).
     * @param estadoNuevo     Nuevo estado.
     * @param idUsuario       ID del usuario que realizó el cambio
     *                        (null si fue automático).
     */
    private void registrarHistorial(Solicitud solicitud,
                                     EstadoSolicitud estadoAnterior,
                                     EstadoSolicitud estadoNuevo,
                                     Long idUsuario) {
        SolicitudHistorialEstado historial = new SolicitudHistorialEstado();
        historial.setSolicitud(solicitud);
        historial.setEstadoAnterior(
            estadoAnterior != null ? estadoAnterior.name() : null
        );
        historial.setEstadoNuevo(estadoNuevo.name());
        historial.setIdUsuarioInterno(idUsuario);
        historialRepository.save(historial);
    }
}