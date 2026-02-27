package com.rdam.backend.enums;

/**
 * Roles posibles para un UsuarioInterno.
 *
 * - OPERADOR: gestiona solicitudes de su circunscripción asignada.
 *             Puede cargar certificados y regenerar tokens.
 *             NO puede ver solicitudes de otras circunscripciones.
 *
 * - ADMIN:    acceso total a todas las circunscripciones.
 *             Puede crear y desactivar usuarios internos.
 *             No tiene circunscripción asignada (null en DB).
 *
 * Spring Security usará estos valores como "authorities"
 * en las anotaciones @PreAuthorize("hasRole('ADMIN')").
 */
public enum RolUsuario {
    OPERADOR,
    ADMIN
}