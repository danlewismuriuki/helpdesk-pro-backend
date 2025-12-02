package com.helpdeskpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API Response wrapper
 * Provides consistent response structure across all endpoints
 *
 * Success Response:
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... },
 *   "statusCode": 200
 * }
 *
 * Error Response:
 * {
 *   "success": false,
 *   "message": "Error message",
 *   "data": null,
 *   "statusCode": 400
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;

    // Constructor without statusCode (for backward compatibility)
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.statusCode = null;
    }

    // Constructor with data but no message
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.message = null;
        this.data = data;
        this.statusCode = null;
    }

    // ========================================================================
    // SUCCESS RESPONSE BUILDERS
    // ========================================================================

    /**
     * Create success response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, 200);
    }

    /**
     * Create success response with only data (default message)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data, 200);
    }

    /**
     * Create success response with only message (no data)
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, 200);
    }

    /**
     * Create success response with no message or data
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "Operation successful", null, 200);
    }

    // ========================================================================
    // ERROR RESPONSE BUILDERS
    // ========================================================================

    /**
     * Create error response with message and status code
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return new ApiResponse<>(false, message, null, statusCode);
    }

    /**
     * Create error response with message, status code, and error data
     */
    public static <T> ApiResponse<T> error(String message, int statusCode, T errorData) {
        return new ApiResponse<>(false, message, errorData, statusCode);
    }

    /**
     * Create error response with only message (default 400 status)
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, 400);
    }

    /**
     * Create bad request error (400)
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, message, null, 400);
    }

    /**
     * Create unauthorized error (401)
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, message, null, 401);
    }

    /**
     * Create forbidden error (403)
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, message, null, 403);
    }

    /**
     * Create not found error (404)
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, message, null, 404);
    }

    /**
     * Create internal server error (500)
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(false, message, null, 500);
    }
}