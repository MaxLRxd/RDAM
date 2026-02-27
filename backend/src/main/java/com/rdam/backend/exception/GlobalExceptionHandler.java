package com.rdam.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Intercepta todas las excepciones de la aplicación y las
 * convierte en respuestas RFC 7807 ProblemDetail.
 *
 * RFC 7807 es el estándar para errores HTTP estructurados:
 * {
 *   "type": "about:blank",
 *   "title": "No encontrado",
 *   "status": 404,
 *   "detail": "Solicitud no encontrada: RDAM-20260201-0042",
 *   "instance": "/api/v1/solicitudes/RDAM-20260201-0042"
 * }
 *
 * NUNCA exponemos stacktraces al cliente.
 * NUNCA devolvemos datos sensibles en el detalle del error.
 *
 * @RestControllerAdvice: intercepta excepciones de todos
 * los @RestController del proyecto.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 — Validación de campos del DTO (@Valid falló)
     * Se acumula el primer error de validación encontrado.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String detalle = ex.getBindingResult()
                           .getFieldErrors()
                           .stream()
                           .findFirst()
                           .map(e -> e.getField() + ": " + e.getDefaultMessage())
                           .orElse("Error de validación");

        return buildProblem(HttpStatus.BAD_REQUEST,
                           "Error de validación",
                           detalle,
                           request.getRequestURI());
    }

    /**
     * 400 — Transición de estado no permitida
     */
    @ExceptionHandler(EstadoInvalidoException.class)
    public ProblemDetail handleEstadoInvalido(
            EstadoInvalidoException ex,
            HttpServletRequest request) {

        return buildProblem(HttpStatus.BAD_REQUEST,
                           "Estado inválido",
                           ex.getMessage(),
                           request.getRequestURI());
    }

    /**
     * 401 — Token inválido o expirado
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ProblemDetail handleTokenInvalido(
            TokenInvalidoException ex,
            HttpServletRequest request) {

        return buildProblem(HttpStatus.UNAUTHORIZED,
                           "Token inválido",
                           ex.getMessage(),
                           request.getRequestURI());
    }

    /**
     * 403 — El operador intenta acceder a otra circunscripción
     */
    @ExceptionHandler(CircunscripcionMismatchException.class)
    public ProblemDetail handleCircunscripcion(
            CircunscripcionMismatchException ex,
            HttpServletRequest request) {

        return buildProblem(HttpStatus.FORBIDDEN,
                           "Acceso denegado",
                           ex.getMessage(),
                           request.getRequestURI());
    }

    /**
     * 404 — Solicitud no encontrada
     */
    @ExceptionHandler(SolicitudNotFoundException.class)
    public ProblemDetail handleNotFound(
            SolicitudNotFoundException ex,
            HttpServletRequest request) {

        return buildProblem(HttpStatus.NOT_FOUND,
                           "No encontrado",
                           ex.getMessage(),
                           request.getRequestURI());
    }

    /**
     * 409 — Conflicto de concurrencia optimista (@Version)
     * Hibernate lanza esta excepción cuando dos threads
     * intentan modificar el mismo registro simultáneamente.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrencia(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {

        return buildProblem(HttpStatus.CONFLICT,
                           "Conflicto de concurrencia",
                           "El recurso fue modificado por otra operación. " +
                           "Por favor reintentá la operación.",
                           request.getRequestURI());
    }

    /**
     * 500 — Cualquier excepción no contemplada.
     * Loguea el error real pero NO lo expone al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(
            Exception ex,
            HttpServletRequest request) {

        // El stacktrace real va al log del servidor, nunca al cliente
        ex.printStackTrace();

        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                           "Error interno",
                           "Ocurrió un error inesperado. " +
                           "Por favor contactá a soporte.",
                           request.getRequestURI());
    }

    // -------------------------
    // Helper
    // -------------------------

    private ProblemDetail buildProblem(HttpStatus status,
                                       String title,
                                       String detail,
                                       String instance) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(instance));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}