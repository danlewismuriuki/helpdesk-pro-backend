package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateCommentRequest;
import com.helpdeskpro.backend.dto.request.UpdateCommentRequest;
import com.helpdeskpro.backend.dto.response.CommentResponse;
import com.helpdeskpro.backend.entity.Comment;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.CommentRepository;
import com.helpdeskpro.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comment Service
 * Handles all comment-related business logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserService userService;

    /**
     * Create a new comment on a ticket
     * Business Rule: Only ticket creator, assigned agent, or admins can comment
     */
    public CommentResponse createComment(Long ticketId, CreateCommentRequest request, Long userId) {
        log.info("Creating comment on ticket {} by user {}", ticketId, userId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User user = userService.getUserEntityById(userId);

        // Validate user can comment on this ticket
        if (!canCommentOnTicket(user, ticket)) {
            throw new InvalidOperationException(
                    "You do not have permission to comment on this ticket. " +
                            "Only the ticket creator, assigned agent, or admins can comment."
            );
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .ticket(ticket)
                .createdBy(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created successfully: ID {}", savedComment.getId());

        return mapToResponse(savedComment);
    }

    /**
     * Get all comments for a ticket
     * Returns comments in chronological order (oldest first)
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicketId(Long ticketId, Long requesterId) {
        log.info("Fetching comments for ticket {}", ticketId);

        // Verify ticket exists
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User requester = userService.getUserEntityById(requesterId);

        // Validate user can view this ticket's comments
        if (!canViewTicket(requester, ticket)) {
            throw new InvalidOperationException("You do not have permission to view this ticket's comments");
        }

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        log.info("Found {} comments for ticket {}", comments.size(), ticketId);

        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a comment
     * Business Rule: Only the comment creator can update their own comment
     */
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        log.info("Updating comment {} by user {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User user = userService.getUserEntityById(userId);

        // Only comment creator or admin can update
        if (!comment.getCreatedBy().getId().equals(userId) && user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("You can only update your own comments");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        log.info("Comment {} updated successfully", commentId);

        return mapToResponse(updatedComment);
    }

    /**
     * Delete a comment (soft delete)
     * Business Rule: Only comment creator or admin can delete
     */
    public void deleteComment(Long commentId, Long userId) {
        log.info("Deleting comment {} by user {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User user = userService.getUserEntityById(userId);

        // Only comment creator or admin can delete
        if (!comment.getCreatedBy().getId().equals(userId) && user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("You can only delete your own comments");
        }

        comment.softDelete();
        commentRepository.save(comment);

        log.info("Comment {} soft deleted successfully", commentId);
    }

    /**
     * Get comment count for a ticket
     */
    @Transactional(readOnly = true)
    public Long getCommentCount(Long ticketId) {
        return commentRepository.countByTicketId(ticketId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Check if user can comment on ticket
     * Rules:
     * - Admins can comment on any ticket
     * - Agents can comment on tickets assigned to them
     * - Customers can comment on tickets they created
     */
    private boolean canCommentOnTicket(User user, Ticket ticket) {
        // Admins can comment on anything
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Agents can comment on tickets assigned to them
        if (user.getRole() == UserRole.AGENT) {
            return ticket.getAssignedTo() != null &&
                    ticket.getAssignedTo().getId().equals(user.getId());
        }

        // Customers can comment on tickets they created
        return ticket.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Check if user can view ticket
     * Same rules as canCommentOnTicket but includes viewing assigned tickets for agents
     */
    private boolean canViewTicket(User user, Ticket ticket) {
        // Admins can view anything
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Agents can view tickets assigned to them or unassigned tickets
        if (user.getRole() == UserRole.AGENT) {
            return ticket.getAssignedTo() == null ||
                    ticket.getAssignedTo().getId().equals(user.getId());
        }

        // Customers can view tickets they created
        return ticket.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .ticketId(comment.getTicket().getId())
                .createdBy(userService.getUserById(comment.getCreatedBy().getId()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isEdited(!comment.getCreatedAt().equals(comment.getUpdatedAt()))
                .build();
    }
}