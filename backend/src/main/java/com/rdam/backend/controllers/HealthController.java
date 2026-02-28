package com.rdam.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check del sistema.
 *
 * GET /health
 *
 * Público. Consultado por el load balancer y el monitoreo.
 * Verifica conectividad con DB y Redis.
 *
 * HTTP 200: { status: "UP", db: "UP", redis: "UP" }
 * HTTP 503: si alguna dependencia crítica falla.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource           dataSource;
    private final StringRedisTemplate  redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        boolean allUp = true;

        // Verificar DB
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2); // timeout 2 segundos
            status.put("db", "UP");
        } catch (Exception e) {
            status.put("db", "DOWN");
            allUp = false;
        }

        // Verificar Redis
        try {
            redisTemplate.getConnectionFactory()
                         .getConnection()
                         .ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN");
            allUp = false;
        }

        status.put("status", allUp ? "UP" : "DOWN");

        // Insertar status al principio del mapa para mejor legibilidad
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", status.remove("status"));
        response.putAll(status);

        return allUp
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(503).body(response);
    }
}