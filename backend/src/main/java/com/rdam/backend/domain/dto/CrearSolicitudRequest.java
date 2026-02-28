package com.rdam.backend.domain.dto;


import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que el ciudadano envía para crear una solicitud.
 * POST /api/v1/solicitudes
 *
 * @NotBlank: el campo no puede ser null ni vacío.
 * @Pattern: valida el formato con regex.
 * Las anotaciones de Bean Validation se activan con
 * @Valid en el controlador.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearSolicitudRequest {

    /**
     * DNI (7-8 dígitos) o CUIL (11 dígitos).
     * Regex: solo dígitos, entre 7 y 11 caracteres.
     */
    @NotBlank(message = "El DNI/CUIL es obligatorio")
    @Pattern(
        regexp = "^[0-9]{7,11}$",
        message = "El DNI/CUIL debe contener entre 7 y 11 dígitos"
    )
    private String dniCuil;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    @Size(max = 255, message = "El email no puede superar 255 caracteres")
    private String email;

    /**
     * ID de la circunscripción judicial (1 al 5).
     * El ciudadano lo selecciona en el dropdown del formulario.
     */
    @NotNull(message = "La circunscripción es obligatoria")
    @Min(value = 1, message = "La circunscripción debe estar entre 1 y 5")
    @Max(value = 5, message = "La circunscripción debe estar entre 1 y 5")
    private Integer idCircunscripcion;
}