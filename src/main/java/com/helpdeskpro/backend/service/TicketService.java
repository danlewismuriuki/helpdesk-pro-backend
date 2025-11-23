package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdateTicketRequest;
import com.helpdeskpro.backend.dto.response.TicketResponse;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;

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
            ticket.setStatus(request.getStatus());
            if (request.getStatus() == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
            ticket.setSlaDeadline(calculateSlaDeadline(request.getPriority()));
        }
        if (request.getAssignedToId() != null) {
            User agent = userService.getUserEntityById(request.getAssignedToId());
            ticket.setAssignedTo(agent);
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
