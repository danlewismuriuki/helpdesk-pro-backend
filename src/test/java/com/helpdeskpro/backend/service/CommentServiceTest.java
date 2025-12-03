package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateCommentRequest;
import com.helpdeskpro.backend.dto.request.UpdateCommentRequest;
import com.helpdeskpro.backend.dto.response.CommentResponse;
import com.helpdeskpro.backend.dto.response.UserResponse;
import com.helpdeskpro.backend.entity.Comment;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.CommentRepository;
import com.helpdeskpro.backend.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService - Complete Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    private User customer;
    private User agent;
    private User admin;
    private User otherCustomer;
    private Ticket ticket;
    private Comment comment;
    private CreateCommentRequest createRequest;
    private UpdateCommentRequest updateRequest;
    private UserResponse customerResponse;
    private UserResponse agentResponse;
    private UserResponse adminResponse;

    @BeforeEach
    void setUp() {
        // Setup users
        customer = User.builder()
                .id(1L)
                .username("customer")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        agent = User.builder()
                .id(2L)
                .username("agent")
                .email("agent@test.com")
                .role(UserRole.AGENT)
                .build();

        admin = User.builder()
                .id(3L)
                .username("admin")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        otherCustomer = User.builder()
                .id(4L)
                .username("other")
                .email("other@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        // Setup user responses
        customerResponse = UserResponse.builder()
                .id(1L)
                .username("customer")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        agentResponse = UserResponse.builder()
                .id(2L)
                .username("agent")
                .email("agent@test.com")
                .role(UserRole.AGENT)
                .build();

        adminResponse = UserResponse.builder()
                .id(3L)
                .username("admin")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        // Setup ticket
        ticket = Ticket.builder()
                .id(1L)
                .title("Test Ticket")
                .createdBy(customer)
                .assignedTo(agent)
                .build();

        // Setup comment
        comment = Comment.builder()
                .id(1L)
                .content("Test comment")
                .ticket(ticket)
                .createdBy(customer)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup requests
        createRequest = CreateCommentRequest.builder()
                .content("This is a new comment")
                .build();

        updateRequest = UpdateCommentRequest.builder()
                .content("This is updated content")
                .build();
    }

    // ==================== CREATE COMMENT TESTS ====================

    @Test
    @DisplayName("Should create comment when ticket creator comments")
    void createComment_WhenTicketCreator_ShouldSucceed() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(1L)).thenReturn(customer);
        when(userService.getUserById(1L)).thenReturn(customerResponse);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = commentService.createComment(1L, createRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should create comment when assigned agent comments")
    void createComment_WhenAssignedAgent_ShouldSucceed() {
        // Given
        comment.setCreatedBy(agent); // Agent is creating this comment
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(2L)).thenReturn(agent);
        when(userService.getUserById(2L)).thenReturn(agentResponse);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = commentService.createComment(1L, createRequest, 2L);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should create comment when admin comments")
    void createComment_WhenAdmin_ShouldSucceed() {
        // Given
        comment.setCreatedBy(admin); // Admin is creating this comment
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(3L)).thenReturn(admin);
        when(userService.getUserById(3L)).thenReturn(adminResponse);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = commentService.createComment(1L, createRequest, 3L);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user tries to comment")
    void createComment_WhenUnauthorized_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(4L)).thenReturn(otherCustomer);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(1L, createRequest, 4L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("do not have permission to comment");

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when ticket not found")
    void createComment_WhenTicketNotFound_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(999L, createRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket");
    }

    // ==================== GET COMMENTS TESTS ====================

    @Test
    @DisplayName("Should get comments for ticket")
    void getCommentsByTicketId_WhenAuthorized_ShouldReturnComments() {
        // Given
        Comment comment2 = Comment.builder()
                .id(2L)
                .content("Second comment")
                .ticket(ticket)
                .createdBy(agent)
                .createdAt(LocalDateTime.now().plusMinutes(5))
                .updatedAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(1L)).thenReturn(customer);
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L))
                .thenReturn(Arrays.asList(comment, comment2));
        when(userService.getUserById(1L)).thenReturn(customerResponse);
        when(userService.getUserById(2L)).thenReturn(agentResponse);

        // When
        List<CommentResponse> comments = commentService.getCommentsByTicketId(1L, 1L);

        // Then
        assertThat(comments).hasSize(2);
        verify(commentRepository).findByTicketIdOrderByCreatedAtAsc(1L);
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user tries to view comments")
    void getCommentsByTicketId_WhenUnauthorized_ShouldThrowException() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userService.getUserEntityById(4L)).thenReturn(otherCustomer);

        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByTicketId(1L, 4L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("do not have permission to view");
    }

    // ==================== UPDATE COMMENT TESTS ====================

    @Test
    @DisplayName("Should update comment when owner updates")
    void updateComment_WhenOwner_ShouldSucceed() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(1L)).thenReturn(customer);
        when(userService.getUserById(1L)).thenReturn(customerResponse);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = commentService.updateComment(1L, updateRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
        assertThat(comment.getContent()).isEqualTo(updateRequest.getContent());
    }

    @Test
    @DisplayName("Should allow admin to update any comment")
    void updateComment_WhenAdmin_ShouldSucceed() {
        // Given
        comment.setCreatedBy(customer); // Comment was created by customer
        comment.setContent("Original content");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(3L)).thenReturn(admin);
        when(userService.getUserById(1L)).thenReturn(customerResponse); // Only stub the comment author
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = commentService.updateComment(1L, updateRequest, 3L);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to update")
    void updateComment_WhenNonOwner_ShouldThrowException() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(2L)).thenReturn(agent);

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(1L, updateRequest, 2L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("can only update your own comments");
    }

    @Test
    @DisplayName("Should throw exception when comment not found")
    void updateComment_WhenNotFound_ShouldThrowException() {
        // Given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(999L, updateRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment");
    }

    // ==================== DELETE COMMENT TESTS ====================

    @Test
    @DisplayName("Should delete comment when owner deletes")
    void deleteComment_WhenOwner_ShouldSucceed() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(1L)).thenReturn(customer);

        // When
        commentService.deleteComment(1L, 1L);

        // Then
        verify(commentRepository).save(any(Comment.class));
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should allow admin to delete any comment")
    void deleteComment_WhenAdmin_ShouldSucceed() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(3L)).thenReturn(admin);

        // When
        commentService.deleteComment(1L, 3L);

        // Then
        verify(commentRepository).save(any(Comment.class));
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to delete")
    void deleteComment_WhenNonOwner_ShouldThrowException() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userService.getUserEntityById(2L)).thenReturn(agent);

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("can only delete your own comments");
    }

    // ==================== COMMENT COUNT TEST ====================

    @Test
    @DisplayName("Should return comment count for ticket")
    void getCommentCount_ShouldReturnCount() {
        // Given
        when(commentRepository.countByTicketId(1L)).thenReturn(5L);

        // When
        Long count = commentService.getCommentCount(1L);

        // Then
        assertThat(count).isEqualTo(5L);
        verify(commentRepository).countByTicketId(1L);
    }
}