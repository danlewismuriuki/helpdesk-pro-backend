package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateTicketRequest;
import com.helpdeskpro.backend.dto.request.UpdateTicketRequest;
import com.helpdeskpro.backend.dto.response.TicketResponse;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - Complete Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TicketService ticketService;

    private User admin;
    private User agent;
    private User customer;
    private Ticket openTicket;
    private CreateTicketRequest createRequest;

    @BeforeEach
    void setUp() {
        // Setup test users
        admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        agent = User.builder()
                .id(2L)
                .username("agent")
                .email("agent@test.com")
                .role(UserRole.AGENT)
                .build();

        customer = User.builder()
                .id(3L)
                .username("customer")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        // Setup test ticket
        openTicket = Ticket.builder()
                .id(1L)
                .title("Test Ticket")
                .description("Test Description")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .createdBy(customer)
                .slaDeadline(LocalDateTime.now().plusDays(3))
                .build();

        // Setup create request
        createRequest = CreateTicketRequest.builder()
                .title("New Ticket")
                .description("New ticket description")
                .priority(TicketPriority.HIGH)
                .build();
    }

    // ==================== CREATE TICKET TESTS ====================

    @Test
    @DisplayName("Should create ticket successfully")
    void createTicket_WithValidData_ShouldSucceed() {
        // Given
        when(userService.getUserEntityById(3L)).thenReturn(customer);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(3L)).thenReturn(null); // Simplified

        // When
        TicketResponse response = ticketService.createTicket(createRequest, 3L);

        // Then
        assertThat(response).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
        verify(userService).getUserEntityById(3L);
    }

    @Test
    @DisplayName("Should set correct SLA deadline based on priority")
    void createTicket_ShouldSetCorrectSLA() {
        // Given
        when(userService.getUserEntityById(3L)).thenReturn(customer);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            assertThat(ticket.getSlaDeadline()).isNotNull();
            return ticket;
        });
        when(userService.getUserById(any())).thenReturn(null);

        // When
        ticketService.createTicket(createRequest, 3L);

        // Then
        verify(ticketRepository).save(argThat(ticket ->
                ticket.getSlaDeadline() != null &&
                        ticket.getStatus() == TicketStatus.OPEN
        ));
    }

    // ==================== ASSIGNMENT TESTS ====================

    @Test
    @DisplayName("Should assign ticket to agent successfully")
    void assignTicket_WhenValidAgent_ShouldSucceed() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(2L)).thenReturn(agent);
        when(userService.getUserEntityById(1L)).thenReturn(admin);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        TicketResponse result = ticketService.assignTicket(1L, 2L, 1L);

        // Then
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(openTicket.getAssignedTo()).isEqualTo(agent);
        assertThat(openTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should throw exception when assigning to non-agent")
    void assignTicket_WhenNonAgent_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(3L)).thenReturn(customer);
        when(userService.getUserEntityById(1L)).thenReturn(admin);

        // When & Then
        assertThatThrownBy(() -> ticketService.assignTicket(1L, 3L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not an agent");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when customer tries to assign")
    void assignTicket_WhenCustomerAssigns_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(2L)).thenReturn(agent);
        when(userService.getUserEntityById(3L)).thenReturn(customer);

        // When & Then
        assertThatThrownBy(() -> ticketService.assignTicket(1L, 2L, 3L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("do not have permission to assign");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should unassign ticket successfully")
    void unassignTicket_WhenAssigned_ShouldSucceed() {
        // Given
        openTicket.setAssignedTo(agent);
        openTicket.setStatus(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(1L)).thenReturn(admin);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        ticketService.unassignTicket(1L, 1L);

        // Then
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(openTicket.getAssignedTo()).isNull();
        assertThat(openTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("Should throw exception when unassigning unassigned ticket")
    void unassignTicket_WhenNotAssigned_ShouldThrowException() {
        // Given
        openTicket.setAssignedTo(null);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(1L)).thenReturn(admin);

        // When & Then
        assertThatThrownBy(() -> ticketService.unassignTicket(1L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not assigned to anyone");
    }

    // ==================== PRIORITY TESTS ====================

    @Test
    @DisplayName("Should update priority and recalculate SLA")
    void updatePriority_ShouldRecalculateSLA() {
        // Given
        LocalDateTime oldSla = openTicket.getSlaDeadline();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(1L)).thenReturn(admin);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        ticketService.updatePriority(1L, TicketPriority.CRITICAL, 1L);

        // Then
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(openTicket.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(openTicket.getSlaDeadline()).isNotEqualTo(oldSla);
    }

    // ==================== STATUS WORKFLOW TESTS ====================

    @Test
    @DisplayName("Should update status from OPEN to IN_PROGRESS")
    void updateStatus_FromOpenToInProgress_ShouldSucceed() {
        // Given
        openTicket.setAssignedTo(agent);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(2L)).thenReturn(agent);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        ticketService.updateStatus(1L, TicketStatus.IN_PROGRESS, 2L);

        // Then
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(openTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should throw exception when moving to IN_PROGRESS without assignment")
    void updateStatus_ToInProgressWithoutAssignment_ShouldThrowException() {
        // Given
        openTicket.setAssignedTo(null);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(1L)).thenReturn(admin);

        // When & Then
        assertThatThrownBy(() -> ticketService.updateStatus(1L, TicketStatus.IN_PROGRESS, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must be assigned");
    }

    @Test
    @DisplayName("Should throw exception when customer tries to resolve")
    void updateStatus_CustomerResolving_ShouldThrowException() {
        // Given
        openTicket.setStatus(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(3L)).thenReturn(customer);

        // When & Then
        assertThatThrownBy(() -> ticketService.updateStatus(1L, TicketStatus.RESOLVED, 3L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only agents can resolve");
    }

    @Test
    @DisplayName("Should set resolvedAt when resolving ticket")
    void updateStatus_ToResolved_ShouldSetResolvedAt() {
        // Given
        openTicket.setStatus(TicketStatus.IN_PROGRESS);
        openTicket.setAssignedTo(agent);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(2L)).thenReturn(agent);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(openTicket);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        ticketService.updateStatus(1L, TicketStatus.RESOLVED, 2L);

        // Then
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(openTicket.getResolvedAt()).isNotNull();
        assertThat(openTicket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("Should throw exception for invalid status transition")
    void updateStatus_InvalidTransition_ShouldThrowException() {
        // Given
        openTicket.setStatus(TicketStatus.CLOSED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserEntityById(1L)).thenReturn(admin);

        // When & Then
        assertThatThrownBy(() -> ticketService.updateStatus(1L, TicketStatus.OPEN, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // ==================== GET TICKETS TESTS ====================

    @Test
    @DisplayName("Should get ticket by ID")
    void getTicketById_WhenExists_ShouldReturnTicket() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));
        when(userService.getUserById(any())).thenReturn(null);

        // When
        TicketResponse response = ticketService.getTicketById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(ticketRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when ticket not found")
    void getTicketById_WhenNotExists_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.getTicketById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket");
    }

    @Test
    @DisplayName("Should get all tickets with pagination")
    void getAllTickets_ShouldReturnPagedResults() {
        // Given
        Page<Ticket> page = new PageImpl<>(Arrays.asList(openTicket));
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userService.getUserById(any())).thenReturn(null);

        // When
        Page<TicketResponse> result = ticketService.getAllTickets(Pageable.unpaged());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should soft delete ticket")
    void deleteTicket_ShouldSetDeletedAt() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(openTicket));

        // When
        ticketService.deleteTicket(1L);

        // Then
        verify(ticketRepository).save(argThat(ticket ->
                ticket.getDeletedAt() != null
        ));
    }

    // ==================== SLA TESTS ====================

    @Test
    @DisplayName("Should get SLA breached tickets")
    void getSlaBreachedTickets_ShouldReturnBreachedTickets() {
        // Given
        Ticket breachedTicket = Ticket.builder()
                .id(2L)
                .title("Breached Ticket")
                .status(TicketStatus.OPEN)
                .slaDeadline(LocalDateTime.now().minusHours(1))
                .createdBy(customer)
                .build();

        when(ticketRepository.findAll()).thenReturn(Arrays.asList(openTicket, breachedTicket));
        when(userService.getUserById(any())).thenReturn(null);

        // When
        List<TicketResponse> breached = ticketService.getSlaBreachedTickets();

        // Then
        assertThat(breached).hasSize(1);
    }
}