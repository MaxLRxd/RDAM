package com.rdam.backend.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Gestiona el almacenamiento de certificados PDF en MinIO.
 *
 * Responsabilidades:
 *   - Subir archivos PDF al bucket privado de MinIO
 *   - Generar tokens de descarga criptográficos de 64 chars
 *   - Eliminar archivos al vencer (llamado por VencimientoScheduler)
 *   - Devolver el stream del PDF para la descarga del ciudadano
 *
 * Los archivos en MinIO son privados. El ciudadano nunca
 * accede directamente: el backend actúa como proxy usando
 * el token_descarga como factor de autenticación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadoService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${rdam.negocio.max-upload-size-bytes}")
    private long maxUploadSizeBytes;

    /**
     * Sube un archivo PDF a MinIO y devuelve la ruta interna.
     *
     * La ruta en MinIO sigue el patrón:
     *   certificados/{idSolicitud}/{uuid}.pdf
     *
     * Esto organiza los archivos por solicitud y evita colisiones
     * de nombres si el operador sube un archivo con el mismo nombre.
     *
     * @param idSolicitud ID de la solicitud asociada.
     * @param archivo     Archivo PDF recibido del operador.
     * @return Ruta interna en MinIO (se guarda en url_certificado).
     * @throws IllegalArgumentException si el archivo no es PDF o supera el tamaño.
     */
    public String subirCertificado(Long idSolicitud,
                                   MultipartFile archivo)
            throws IOException {

        // Validar tipo de archivo
        if (!"application/pdf".equals(archivo.getContentType())) {
            throw new IllegalArgumentException(
                "El archivo debe ser un PDF. Tipo recibido: "
                + archivo.getContentType()
            );
        }

        // Validar tamaño (10 MB máx según SPEC)
        if (archivo.getSize() > maxUploadSizeBytes) {
            throw new IllegalArgumentException(
                "El archivo supera el tamaño máximo permitido de 10 MB."
            );
        }

        // Construir ruta única en MinIO
        String nombreArchivo = UUID.randomUUID().toString() + ".pdf";
        String rutaMinIO = "certificados/" + idSolicitud + "/" + nombreArchivo;

        try (InputStream inputStream = archivo.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(rutaMinIO)
                    .stream(inputStream, archivo.getSize(), -1)
                    .contentType("application/pdf")
                    .build()
            );
        } catch (MinioException | InvalidKeyException |
                 NoSuchAlgorithmException e) {
            log.error("Error subiendo certificado a MinIO. " +
                      "idSolicitud={} error={}", idSolicitud, e.getMessage());
            throw new IOException("Error al almacenar el certificado.", e);
        }

        log.info("Certificado subido a MinIO. idSolicitud={} ruta={}",
                idSolicitud, rutaMinIO);

        return rutaMinIO;
    }

    /**
     * Genera un token de descarga criptográfico de 64 caracteres.
     * Igual que generarToken64() de TokenService pero este vive
     * en DB (columna token_descarga), no en Redis.
     *
     * Se llama cada vez que el operador sube un certificado
     * o regenera el token.
     */
    public String generarTokenDescarga() {
        return UUID.randomUUID().toString().replace("-", "")
             + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Devuelve el stream del PDF para que el controlador
     * lo escriba en la respuesta HTTP.
     *
     * El controlador es responsable de cerrar el stream
     * después de enviarlo al cliente.
     *
     * @param rutaMinIO Ruta interna del archivo en MinIO
     *                  (valor de url_certificado en DB).
     * @return InputStream del archivo PDF.
     */
    public InputStream obtenerCertificado(String rutaMinIO)
            throws IOException {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(rutaMinIO)
                    .build()
            );
        } catch (MinioException | InvalidKeyException |
                 NoSuchAlgorithmException e) {
            log.error("Error obteniendo certificado de MinIO. " +
                      "ruta={} error={}", rutaMinIO, e.getMessage());
            throw new IOException("Error al obtener el certificado.", e);
        }
    }

    /**
     * Elimina el archivo PDF de MinIO al vencer el certificado.
     * Llamado por VencimientoScheduler cuando una solicitud
     * PUBLICADO supera los 65 días (PRD) / 80 días (DEV).
     *
     * Si el archivo ya no existe en MinIO, logueamos warning
     * pero no lanzamos excepción (podría haberse eliminado
     * manualmente por un admin).
     *
     * @param rutaMinIO Ruta interna del archivo en MinIO.
     */
    public void eliminarCertificado(String rutaMinIO) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(rutaMinIO)
                    .build()
            );
            log.info("Certificado eliminado de MinIO. ruta={}", rutaMinIO);

        } catch (MinioException | InvalidKeyException |
                 NoSuchAlgorithmException | IOException e) {
            log.warn("No se pudo eliminar certificado de MinIO. " +
                     "ruta={} error={}", rutaMinIO, e.getMessage());
        }
    }
}