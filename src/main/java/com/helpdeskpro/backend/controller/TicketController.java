package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.AssignTicketRequest;
import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdatePriorityRequest;
import com.helpdeskpro.backend.dto.request.UpdateStatusRequest;
import com.helpdeskpro.backend.dto.request.UpdateTicketRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.TicketResponse;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.security.CurrentUser;
import com.helpdeskpro.backend.security.UserPrincipal;
import com.helpdeskpro.backend.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tickets", description = "Ticket management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TicketService ticketService;

    // ==================== EXISTING ENDPOINTS ====================

    @PostMapping
    @Operation(summary = "Create a new ticket", description = "Any authenticated user can create a ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("Creating ticket for user: {}", currentUser.getId());
        TicketResponse ticket = ticketService.createTicket(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket, "Ticket created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieve a specific ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all tickets", description = "Admins and agents can view all tickets")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getAllTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> tickets = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get tickets by status", description = "Filter tickets by status")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getTicketsByStatus(
            @PathVariable TicketStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> tickets = ticketService.getTicketsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "Get my tickets", description = "Get tickets created by current user")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> tickets = ticketService.getMyTickets(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get tickets assigned to me", description = "Get tickets assigned to current agent")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getAssignedTickets(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TicketResponse> tickets = ticketService.getAssignedTickets(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket", description = "Update ticket details")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {

        TicketResponse ticket = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Ticket updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete ticket", description = "Soft delete a ticket (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Ticket deleted successfully"));
    }

    // ==================== NEW: ASSIGNMENT ENDPOINTS ====================

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
            summary = "Assign ticket to agent",
            description = "Assign a ticket to an agent. Only admins and agents can perform this action."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} assigning ticket {} to agent {}",
                currentUser.getId(), id, request.getAgentId());

        TicketResponse ticket = ticketService.assignTicket(
                id,
                request.getAgentId(),
                currentUser.getId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(ticket, "Ticket assigned successfully")
        );
    }

    @PutMapping("/{id}/unassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
            summary = "Unassign ticket",
            description = "Remove assignment from a ticket. Only admins and agents can perform this action."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> unassignTicket(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} unassigning ticket {}", currentUser.getId(), id);

        TicketResponse ticket = ticketService.unassignTicket(id, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(ticket, "Ticket unassigned successfully")
        );
    }

    // ==================== NEW: PRIORITY MANAGEMENT ====================

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
            summary = "Update ticket priority",
            description = "Change ticket priority and recalculate SLA deadline"
    )
    public ResponseEntity<ApiResponse<TicketResponse>> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} updating priority of ticket {} to {}",
                currentUser.getId(), id, request.getPriority());

        TicketResponse ticket = ticketService.updatePriority(
                id,
                request.getPriority(),
                currentUser.getId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(ticket, "Ticket priority updated successfully")
        );
    }

    // ==================== NEW: STATUS WORKFLOW ====================

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Update ticket status",
            description = "Change ticket status following the workflow: OPEN → IN_PROGRESS → RESOLVED → CLOSED"
    )
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} updating status of ticket {} to {}",
                currentUser.getId(), id, request.getStatus());

        TicketResponse ticket = ticketService.updateStatus(
                id,
                request.getStatus(),
                currentUser.getId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(ticket, "Ticket status updated successfully")
        );
    }

    // ==================== NEW: SLA MONITORING ====================

    @GetMapping("/sla-breached")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
            summary = "Get SLA breached tickets",
            description = "Retrieve all tickets that have breached their SLA deadline"
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getSlaBreachedTickets() {
        log.info("Fetching SLA breached tickets");

        List<TicketResponse> tickets = ticketService.getSlaBreachedTickets();

        return ResponseEntity.ok(
                ApiResponse.success(
                        tickets,
                        String.format("Found %d tickets breaching SLA", tickets.size())
                )
        );
    }
}