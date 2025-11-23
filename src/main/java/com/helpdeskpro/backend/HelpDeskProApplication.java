package com.helpdeskpro.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for HelpDesk Pro Backend Application.
 *
 * This is a production-grade Spring Boot REST API for a ticketing system.
 *
 * Features:
 * - JWT Authentication & Authorization
 * - Role-based Access Control (RBAC)
 * - Ticket Management (CRUD operations)
 * - SLA Tracking
 * - Knowledge Base
 * - Admin Dashboard & Analytics
 * - Redis Caching
 * - Database Migrations with Flyway
 * - API Documentation with Swagger/OpenAPI
 * - Comprehensive Testing (Unit + Integration)
 *
 * @author Your Name
 * @version 0.0.1-SNAPSHOT
 * @since 2025-11-22
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class HelpDeskProApplication {

    /**
     * Main method - Application entry point.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HelpDeskProApplication.class, args);

        // Log startup message
        System.out.println("\n" +
                "╔═══════════════════════════════════════════════════════════╗\n" +
                "║                                                           ║\n" +
                "║          HelpDesk Pro Backend - STARTED ✓                ║\n" +
                "║                                                           ║\n" +
                "║  API Documentation: http://localhost:8080/api/swagger-ui.html  ║\n" +
                "║  Health Check:      http://localhost:8080/api/actuator/health  ║\n" +
                "║                                                           ║\n" +
                "╚═══════════════════════════════════════════════════════════╝\n");
    }
}