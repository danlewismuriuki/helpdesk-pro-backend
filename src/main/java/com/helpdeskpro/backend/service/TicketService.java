package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdateTicketRequest;
import com.helpdeskpro.backend.dto.response.TicketResponse;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;

    // ==================== EXISTING METHODS ====================

    public TicketResponse createTicket(CreateTicketRequest request, Long userId) {
        log.info("Creating new ticket: {}", request.getTitle());

        User creator = userService.getUserEntityById(userId);

        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .createdBy(creator)
                .slaDeadline(calculateSlaDeadline(request.getPriority()))
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully: ID {}", savedTicket.getId());

        return mapToResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTickets(Long userId, Pageable pageable) {
        User user = userService.getUserEntityById(userId);
        return ticketRepository.findByCreatedBy(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getAssignedTickets(Long agentId, Pageable pageable) {
        User agent = userService.getUserEntityById(agentId);
        return ticketRepository.findByAssignedTo(agent, pageable)
                .map(this::mapToResponse);
    }

    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        log.info("Updating ticket: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            updateTicketStatus(ticket, request.getStatus());
        }
        if (request.getPriority() != null) {
            updateTicketPriority(ticket, request.getPriority());
        }
        if (request.getAssignedToId() != null) {
            assignTicketToAgent(ticket, request.getAssignedToId());
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket updated successfully: {}", id);

        return mapToResponse(updatedTicket);
    }

    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        ticket.softDelete();
        ticketRepository.save(ticket);
        log.info("Ticket soft deleted: {}", id);
    }

    // ==================== NEW: ASSIGNMENT LOGIC ====================

    /**
     * Assign ticket to an agent
     * Business Rule: Only ADMIN or AGENT roles can be assigned tickets
     */
    public TicketResponse assignTicket(Long ticketId, Long agentId, Long requesterId) {
        log.info("Assigning ticket {} to agent {}", ticketId, agentId);

        Ticket ticket = getTicketEntity(ticketId);
        User agent = userService.getUserEntityById(agentId);
        User requester = userService.getUserEntityById(requesterId);

        // Validate agent role
        if (!isAgentOrAdmin(agent)) {
            throw new InvalidOperationException(
                    "User " + agent.getEmail() + " is not an agent or admin. Only agents can be assigned tickets."
            );
        }

        // Validate requester has permission to assign
        if (!canAssignTickets(requester)) {
            throw new InvalidOperationException(
                    "You do not have permission to assign tickets. Only admins and agents can assign tickets."
            );
        }

        ticket.setAssignedTo(agent);

        // Auto-transition to IN_PROGRESS if still OPEN
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            log.info("Ticket {} status changed to IN_PROGRESS upon assignment", ticketId);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket {} successfully assigned to agent {}", ticketId, agent.getEmail());

        return mapToResponse(savedTicket);
    }

    /**
     * Unassign ticket from current agent
     */
    public TicketResponse unassignTicket(Long ticketId, Long requesterId) {
        log.info("Unassigning ticket {}", ticketId);

        Ticket ticket = getTicketEntity(ticketId);
        User requester = userService.getUserEntityById(requesterId);

        if (!canAssignTickets(requester)) {
            throw new InvalidOperationException("Only admins and agents can unassign tickets");
        }

        if (ticket.getAssignedTo() == null) {
            throw new InvalidOperationException("Ticket is not assigned to anyone");
        }

        ticket.setAssignedTo(null);

        // Revert to OPEN if in IN_PROGRESS
        if (ticket.getStatus() == TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.OPEN);
            log.info("Ticket {} status reverted to OPEN upon unassignment", ticketId);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket {} successfully unassigned", ticketId);

        return mapToResponse(savedTicket);
    }

    // ==================== NEW: PRIORITY MANAGEMENT ====================

    /**
     * Update ticket priority and recalculate SLA
     */
    public TicketResponse updatePriority(Long ticketId, TicketPriority newPriority, Long requesterId) {
        log.info("Updating priority of ticket {} to {}", ticketId, newPriority);

        Ticket ticket = getTicketEntity(ticketId);
        User requester = userService.getUserEntityById(requesterId);

        if (!canModifyTicket(requester, ticket)) {
            throw new InvalidOperationException("You do not have permission to update this ticket's priority");
        }

        TicketPriority oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        // Recalculate SLA deadline based on new priority
        ticket.setSlaDeadline(calculateSlaDeadline(newPriority));

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket {} priority updated from {} to {}, new SLA: {}",
                ticketId, oldPriority, newPriority, savedTicket.getSlaDeadline());

        return mapToResponse(savedTicket);
    }

    // ==================== NEW: STATUS WORKFLOW ====================

    /**
     * Update ticket status with workflow validation
     * Enforces: OPEN → IN_PROGRESS → RESOLVED → CLOSED
     */
    public TicketResponse updateStatus(Long ticketId, TicketStatus newStatus, Long requesterId) {
        log.info("Updating status of ticket {} to {}", ticketId, newStatus);

        Ticket ticket = getTicketEntity(ticketId);
        User requester = userService.getUserEntityById(requesterId);

        // Validate status transition
        validateStatusTransition(ticket, newStatus, requester);

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);

        // Handle status-specific logic
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
            log.info("Ticket {} resolved at {}", ticketId, ticket.getResolvedAt());
        }

        if (newStatus == TicketStatus.IN_PROGRESS && ticket.getAssignedTo() == null) {
            throw new InvalidOperationException(
                    "Cannot move ticket to IN_PROGRESS without assigning it to an agent"
            );
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket {} status updated from {} to {}", ticketId, oldStatus, newStatus);

        return mapToResponse(savedTicket);
    }

    /**
     * Get all tickets breaching SLA
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getSlaBreachedTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> breachedTickets = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getSlaDeadline().isBefore(now)
                        && ticket.getStatus() != TicketStatus.RESOLVED
                        && ticket.getStatus() != TicketStatus.CLOSED)
                .toList();

        return breachedTickets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Ticket getTicketEntity(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
    }

    private void assignTicketToAgent(Ticket ticket, Long agentId) {
        User agent = userService.getUserEntityById(agentId);

        if (!isAgentOrAdmin(agent)) {
            throw new InvalidOperationException(
                    "Cannot assign ticket to non-agent user"
            );
        }

        ticket.setAssignedTo(agent);
    }

    private void updateTicketStatus(Ticket ticket, TicketStatus newStatus) {
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        ticket.setStatus(newStatus);
    }

    private void updateTicketPriority(Ticket ticket, TicketPriority newPriority) {
        ticket.setPriority(newPriority);
        ticket.setSlaDeadline(calculateSlaDeadline(newPriority));
    }

    /**
     * Validates if status transition is allowed
     */
    private void validateStatusTransition(Ticket ticket, TicketStatus newStatus, User requester) {
        TicketStatus currentStatus = ticket.getStatus();

        // Business Rule: Only agents can resolve tickets
        if (newStatus == TicketStatus.RESOLVED && !isAgentOrAdmin(requester)) {
            throw new InvalidOperationException("Only agents can resolve tickets");
        }

        // Prevent invalid backward transitions
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new InvalidOperationException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }

        // Business Rule: Can only move to IN_PROGRESS if assigned
        if (newStatus == TicketStatus.IN_PROGRESS && ticket.getAssignedTo() == null) {
            throw new InvalidOperationException(
                    "Ticket must be assigned to an agent before moving to IN_PROGRESS"
            );
        }
    }

    /**
     * Checks if status transition follows valid workflow
     */
    private boolean isValidStatusTransition(TicketStatus current, TicketStatus next) {
        if (current == null || next == null) {
            return false;
        }

        return switch (current) {
            case OPEN -> next == TicketStatus.IN_PROGRESS || next == TicketStatus.CLOSED;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED || next == TicketStatus.OPEN;
            case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.IN_PROGRESS;
            case CLOSED -> false; // Cannot reopen closed tickets
            default -> false; // Unknown status
        };
    }

    private boolean isAgentOrAdmin(User user) {
        return user.getRole() == UserRole.AGENT || user.getRole() == UserRole.ADMIN;
    }

    private boolean canAssignTickets(User user) {
        return isAgentOrAdmin(user);
    }

    private boolean canModifyTicket(User user, Ticket ticket) {
        // Admins can modify any ticket
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Agents can modify tickets assigned to them or unassigned tickets
        if (user.getRole() == UserRole.AGENT) {
            return ticket.getAssignedTo() == null ||
                    ticket.getAssignedTo().getId().equals(user.getId());
        }

        // Customers can only modify their own tickets
        return ticket.getCreatedBy().getId().equals(user.getId());
    }

    private LocalDateTime calculateSlaDeadline(TicketPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case CRITICAL -> now.plusHours(4);
            case HIGH -> now.plusHours(24);
            case MEDIUM -> now.plusDays(3);
            case LOW -> now.plusDays(7);
        };
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(userService.getUserById(ticket.getCreatedBy().getId()))
                .assignedTo(ticket.getAssignedTo() != null ?
                        userService.getUserById(ticket.getAssignedTo().getId()) : null)
                .slaDeadline(ticket.getSlaDeadline())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .isSlaBreached(ticket.getSlaDeadline().isBefore(LocalDateTime.now()) &&
                        ticket.getStatus() != TicketStatus.RESOLVED)
                .build();
    }
}