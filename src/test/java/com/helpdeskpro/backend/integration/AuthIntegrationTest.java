package com.helpdeskpro.backend.integration;

import com.helpdeskpro.backend.dto.request.LoginRequest;
import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth API Integration Tests")
class AuthIntegrationTest extends BaseIntegrationTest {

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void registerUser_WithValidData_ShouldSucceed() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@test.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").exists())
                // CORRECTED JSON PATHS
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should fail registration with duplicate email")
    void registerUser_WithDuplicateEmail_ShouldFail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("duplicate")
                .email("customer@test.com") // Already exists from setUp
                .password("password123")
                .firstName("Duplicate")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail registration with duplicate username")
    void registerUser_WithDuplicateUsername_ShouldFail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("customer") // Already exists from setUp
                .email("newemail@test.com")
                .password("password123")
                .firstName("Duplicate")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail registration with invalid email")
    void registerUser_WithInvalidEmail_ShouldFail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("invalid-email")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail registration with short password")
    void registerUser_WithShortPassword_ShouldFail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@test.com")
                .password("123") // Too short
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WithValidCredentials_ShouldSucceed() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                // CORRECTED JSON PATHS
                .andExpect(jsonPath("$.data.email").value("customer@test.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void login_WithWrongPassword_ShouldFail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("customer@test.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail login with non-existent email")
    void login_WithNonExistentEmail_ShouldFail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail login with missing email")
    void login_WithMissingEmail_ShouldFail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail login with missing password")
    void login_WithMissingPassword_ShouldFail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("customer@test.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }
}