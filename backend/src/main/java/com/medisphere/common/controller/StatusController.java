package com.medisphere.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * System status controller for health checks and baseline connectivity verification.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MediSphere Cognitive Twin",
                "milestone", "M1",
                "phase", "Phase 1: Project Scaffolding & Infrastructure",
                "timestamp", Instant.now().toString()
        ));
    }
}
