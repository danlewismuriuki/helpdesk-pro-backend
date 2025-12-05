package com.helpdeskpro.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User API Integration Tests")
class UserIntegrationTest extends BaseIntegrationTest {

    // ==================== GET USER TESTS ====================

    @Test
    @DisplayName("User should get their own profile")
    void getProfile_AsAuthenticatedUser_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("customer@test.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should fail to get profile without authentication")
    void getProfile_WithoutAuth_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin should get all users")
    void getAllUsers_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[*].email", hasItem("customer@test.com")))
                .andExpect(jsonPath("$.data[*].email", hasItem("agent@test.com")))
                .andExpect(jsonPath("$.data[*].email", hasItem("admin@test.com")));
    }

    @Test
    @DisplayName("Customer should not get all users")
    void getAllUsers_AsCustomer_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should get user by ID")
    void getUserById_AsAdmin_ShouldSucceed() throws Exception {
        // Get all users first to extract an ID
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(response).get("data").get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    // ==================== GET USERS BY ROLE TESTS ====================

    @Test
    @DisplayName("Admin should get users by role")
    void getUsersByRole_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users/role/AGENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].role", everyItem(is("AGENT"))));
    }

    @Test
    @DisplayName("Should get all agents")
    void getAgents_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users/role/AGENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should get all customers")
    void getCustomers_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users/role/CUSTOMER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    @DisplayName("User should update their own profile")
    void updateProfile_AsOwner_ShouldSucceed() throws Exception {
        String updateJson = """
                {
                    "firstName": "Updated",
                    "lastName": "Name"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));
    }

    @Test
    @DisplayName("Admin should update any user")
    void updateUser_AsAdmin_ShouldSucceed() throws Exception {
        // Get a user ID first
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(response).get("data").get(0).get("id").asText();

        String updateJson = """
                {
                    "firstName": "AdminUpdated",
                    "lastName": "AdminName"
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + userId)
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(updateJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User should not update other user's profile")
    void updateUser_AsNonAdmin_ShouldFail() throws Exception {
        // Get a different user ID
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(response).get("data").get(1).get("id").asText();

        String updateJson = """
                {
                    "firstName": "Unauthorized",
                    "lastName": "Update"
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + userId)
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    @DisplayName("Admin should delete user")
    void deleteUser_AsAdmin_ShouldSucceed() throws Exception {
        // Register a new user to delete
        registerUser("todelete@test.com", "ToDelete", "User", com.helpdeskpro.backend.entity.enums.UserRole.CUSTOMER);

        // Get the user ID
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Find the user with email todelete@test.com
        var users = objectMapper.readTree(response).get("data");
        String userToDeleteId = null;
        for (var user : users) {
            if ("todelete@test.com".equals(user.get("email").asText())) {
                userToDeleteId = user.get("id").asText();
                break;
            }
        }

        mockMvc.perform(delete("/api/v1/users/" + userToDeleteId)
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Customer should not delete users")
    void deleteUser_AsCustomer_ShouldFail() throws Exception {
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(response).get("data").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/users/" + userId)
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Agent should not delete users")
    void deleteUser_AsAgent_ShouldFail() throws Exception {
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(response).get("data").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/users/" + userId)
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }
}