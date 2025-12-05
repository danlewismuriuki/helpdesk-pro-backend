package com.helpdeskpro.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdateTicketRequest;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Ticket API Integration Tests")
class TicketIntegrationTest extends BaseIntegrationTest {

    // ==================== CREATE TICKET TESTS ====================

    @Test
    @DisplayName("Customer should create ticket successfully")
    void createTicket_AsCustomer_ShouldSucceed() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Login Issue")
                .description("Cannot login to my account")
                .priority(TicketPriority.HIGH)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Login Issue"))
                .andExpect(jsonPath("$.data.description").value("Cannot login to my account"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("Should fail to create ticket without authentication")
    void createTicket_WithoutAuth_ShouldFail() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Test Ticket")
                .description("Test Description")
                .priority(TicketPriority.MEDIUM)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail to create ticket with missing title")
    void createTicket_WithMissingTitle_ShouldFail() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .description("Test Description")
                .priority(TicketPriority.MEDIUM)
                .build();

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET TICKET TESTS ====================

    @Test
    @DisplayName("Should get ticket by ID")
    void getTicket_ById_ShouldSucceed() throws Exception {
        // Create a ticket first
        CreateTicketRequest createRequest = CreateTicketRequest.builder()
                .title("Test Ticket")
                .description("Test Description")
                .priority(TicketPriority.MEDIUM)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketId = extractIdFromResponse(createResult);

        // Get the ticket
        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId))
                .andExpect(jsonPath("$.data.title").value("Test Ticket"));
    }

    @Test
    @DisplayName("Should get all tickets for customer")
    void getAllTickets_AsCustomer_ShouldReturnOnlyOwnTickets() throws Exception {
        createTicket("Ticket 1", TicketPriority.HIGH);
        createTicket("Ticket 2", TicketPriority.MEDIUM);

        // FIX: Use the correct endpoint for a customer's own tickets
        mockMvc.perform(get("/api/v1/tickets/my-tickets")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Agent should see all tickets")
    void getAllTickets_AsAgent_ShouldSeeAllTickets() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ==================== UPDATE TICKET TESTS ====================

    @Test
    @DisplayName("Agent should assign ticket to themselves")
    void assignTicket_AsAgent_ShouldSucceed() throws Exception {
        String ticketId = createTicket("Unassigned Ticket", TicketPriority.MEDIUM);

        // 1. Get the agent's ID from their profile
        String response = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String agentId = objectMapper.readTree(response).get("data").get("id").asText();

        // 2. Create the required request body for the assignment
        String requestBody = String.format("{\"agentId\": %s}", agentId);

        // 3. Perform the request with the body
        mockMvc.perform(put("/api/v1/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)) // <-- FIX: Add request body
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTo.id").value(agentId)) // Check the assigned agent's ID
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Customer should not be able to assign tickets")
    void assignTicket_AsCustomer_ShouldFail() throws Exception {
        String ticketId = createTicket("Test Ticket", TicketPriority.MEDIUM);

        mockMvc.perform(put("/api/v1/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should update ticket status")
    void updateTicketStatus_AsAgent_ShouldSucceed() throws Exception {
        String ticketId = createTicket("Test Ticket", TicketPriority.MEDIUM);

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .status(TicketStatus.IN_PROGRESS)
                .build();

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Should update ticket priority")
    void updateTicketPriority_AsAgent_ShouldSucceed() throws Exception {
        String ticketId = createTicket("Test Ticket", TicketPriority.LOW);

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .priority(TicketPriority.CRITICAL)
                .build();

        mockMvc.perform(put("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    // ==================== DELETE TICKET TESTS ====================

    @Test
    @DisplayName("Admin should delete ticket")
    void deleteTicket_AsAdmin_ShouldSucceed() throws Exception {
        String ticketId = createTicket("Ticket to Delete", TicketPriority.LOW);

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify ticket is deleted
        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Customer should not delete tickets")
    void deleteTicket_AsCustomer_ShouldFail() throws Exception {
        String ticketId = createTicket("Test Ticket", TicketPriority.LOW);

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ==================== HELPER METHODS ====================

    private String createTicket(String title, TicketPriority priority) throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title(title)
                .description("Test description for " + title)
                .priority(priority)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
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