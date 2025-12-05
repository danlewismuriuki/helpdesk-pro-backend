package com.helpdeskpro.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpdeskpro.backend.dto.request.CreateCommentRequest;
import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdateCommentRequest;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Comment API Integration Tests")
class CommentIntegrationTest extends BaseIntegrationTest {

    // ==================== CREATE COMMENT TESTS ====================

    @Test
    @DisplayName("Ticket creator should add comment successfully")
    void createComment_AsTicketCreator_ShouldSucceed() throws Exception {
        String ticketId = createTicket();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("This is my comment on my ticket")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("This is my comment on my ticket"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdBy").exists());
    }

    @Test
    @DisplayName("Agent should add comment to assigned ticket")
    void createComment_AsAgent_ShouldSucceed() throws Exception {
        String ticketId = createTicket();

        // 1. Get the agent's ID from their profile
        String response = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String agentId = objectMapper.readTree(response).get("data").get("id").asText();

        // 2. Assign the ticket to the agent with the required request body
        String requestBody = String.format("{\"agentId\": %s}", agentId);
        mockMvc.perform(put("/api/v1/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)) // <-- FIX: Add request body
                .andExpect(status().isOk());

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Agent response to ticket")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Agent response to ticket"));
    }

    @Test
    @DisplayName("Admin should add comment to any ticket")
    void createComment_AsAdmin_ShouldSucceed() throws Exception {
        String ticketId = createTicket();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Admin comment")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Admin comment"));
    }

    @Test
    @DisplayName("Unauthorized user should not add comment")
    void createComment_AsUnauthorizedUser_ShouldFail() throws Exception {
        String ticketId = createTicket();

        // Create another customer
        registerUser("other@test.com", "Other", "User", com.helpdeskpro.backend.entity.enums.UserRole.CUSTOMER);
        String otherToken = loginUser("other@test.com", "password123");

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Unauthorized comment")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to create comment without authentication")
    void createComment_WithoutAuth_ShouldFail() throws Exception {
        String ticketId = createTicket();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Test comment")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail to create empty comment")
    void createComment_WithEmptyContent_ShouldFail() throws Exception {
        String ticketId = createTicket();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("")
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET COMMENTS TESTS ====================

    @Test
    @DisplayName("Should get all comments for ticket")
    void getComments_ForTicket_ShouldReturnAll() throws Exception {
        String ticketId = createTicket();

        // Create multiple comments
        createComment(ticketId, "First comment", customerToken);
        createComment(ticketId, "Second comment", customerToken);

        mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").exists())
                .andExpect(jsonPath("$.data[1].content").exists());
    }

    @Test
    @DisplayName("Should return empty list for ticket with no comments")
    void getComments_ForTicketWithNoComments_ShouldReturnEmpty() throws Exception {
        String ticketId = createTicket();

        mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== UPDATE COMMENT TESTS ====================

    @Test
    @DisplayName("Comment owner should update their comment")
    void updateComment_AsOwner_ShouldSucceed() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Original content", customerToken);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .content("Updated content")
                .build();

        mockMvc.perform(put("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated content"));
    }

    @Test
    @DisplayName("Admin should update any comment")
    void updateComment_AsAdmin_ShouldSucceed() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Original content", customerToken);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .content("Admin updated content")
                .build();

        mockMvc.perform(put("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Admin updated content"));
    }

    @Test
    @DisplayName("Non-owner should not update comment")
    void updateComment_AsNonOwner_ShouldFail() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Original content", customerToken);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .content("Unauthorized update")
                .build();

        mockMvc.perform(put("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE COMMENT TESTS ====================

    @Test
    @DisplayName("Comment owner should delete their comment")
    void deleteComment_AsOwner_ShouldSucceed() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Comment to delete", customerToken);

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Admin should delete any comment")
    void deleteComment_AsAdmin_ShouldSucceed() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Comment to delete", customerToken);

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Non-owner should not delete comment")
    void deleteComment_AsNonOwner_ShouldFail() throws Exception {
        String ticketId = createTicket();
        String commentId = createComment(ticketId, "Comment to delete", customerToken);

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    // ==================== HELPER METHODS ====================

    private String createTicket() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Test Ticket")
                .description("Test Description")
                .priority(TicketPriority.MEDIUM)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractIdFromResponse(result);
    }

    private String createComment(String ticketId, String content, String token) throws Exception {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .content(content)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractIdFromResponse(result);
    }

    private String extractIdFromResponse(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        // Handle ApiResponse wrapper: {"success": true, "data": {...}, ...}
        if (root.has("data")) {
            return root.get("data").get("id").asText();
        }
        // Fallback to direct id if no wrapper
        return root.get("id").asText();
    }
}