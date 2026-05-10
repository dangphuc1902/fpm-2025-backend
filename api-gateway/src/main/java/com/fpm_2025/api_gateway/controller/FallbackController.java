package com.fpm_2025.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "Service Unavailable",
                        "message", "The requested microservice is currently unavailable or taking too long to respond. Please try again later."
                ));
    }
}
