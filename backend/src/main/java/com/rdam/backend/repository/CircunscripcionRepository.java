package com.rdam.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdam.backend.domain.entity.Circunscripcion;

/**
 * Acceso a la tabla maestra de circunscripciones.
 *
 * Solo operaciones de lectura. Los datos se insertan
 * en V6__datos_iniciales.sql y no se modifican desde la API.
 *
 * JpaRepository<Circunscripcion, Integer>:
 *   - Circunscripcion → entidad que maneja
 *   - Integer         → tipo del @Id
 *
 * Métodos heredados que usaremos:
 *   - findById(Integer id)   → buscar una circunscripción por ID
 *   - findAll()              → listar las 5 para el dropdown del frontend
 *   - existsById(Integer id) → validar que el ID enviado en el form existe
 */
@Repository
public interface CircunscripcionRepository
        extends JpaRepository<Circunscripcion, Integer> {
    // JpaRepository ya provee todo lo que necesitamos.
    // No hace falta declarar métodos adicionales.
}