package com.rdam.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa una Circunscripción Judicial
 * de la Provincia de Santa Fe.
 *
 * Tabla: circunscripcion
 * Filas: 5 (Santa Fe, Rosario, Venado Tuerto, Reconquista, Rafaela)
 * Estas filas se insertan mediante el script Flyway V5 y nunca
 * se modifican desde la API.
 *
 * Relaciones:
 *   - Solicitud         (N solicitudes → 1 circunscripcion)
 *   - UsuarioInterno    (N usuarios    → 1 circunscripcion)
 */
@Entity
@Table(name = "circunscripcion")
public class Circunscripcion {

    /**
     * @Id marca el campo como clave primaria.
     * No usamos @GeneratedValue porque el ID lo controlamos
     * nosotros (1 al 5), no lo genera la DB automáticamente.
     */
    @Id
    @Column(name = "id_circunscripcion")
    private Integer id;

    /**
     * Nombre legible: "Santa Fe", "Rosario", etc.
     * Se muestra en la UI y en las respuestas de la API.
     */
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    /**
     * Nombre de la sede física.
     * Ejemplo: "Sede Rosario"
     */
    @Column(name = "sede", nullable = false, length = 100)
    private String sede;

    // -------------------------
    // Constructores
    // -------------------------

    // JPA necesita un constructor sin argumentos (puede ser protected)
    protected Circunscripcion() {}

    public Circunscripcion(Integer id, String nombre, String sede) {
        this.id = id;
        this.nombre = nombre;
        this.sede = sede;
    }

    // -------------------------
    // Getters
    // (sin setters: esta entidad es inmutable desde la app)
    // -------------------------

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSede() { return sede; }

    @Override
    public String toString() {
        return "Circunscripcion{id=" + id + ", nombre='" + nombre + "'}";
    }
}