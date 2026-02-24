package com.onlinedatatepo.data_repository.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home controller for the Online Data Repository application.
 * Provides basic health check and root endpoints.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse home() {
        return new ApiResponse("success", "Online Data Repository API is running!", null);
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Service is healthy", null);
    }

    /**
     * Simple API response wrapper for JSON responses.
     */
    public record ApiResponse(String status, String message, Object data) {}
}
