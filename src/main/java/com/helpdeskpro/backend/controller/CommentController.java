package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.CreateCommentRequest;
import com.helpdeskpro.backend.dto.request.UpdateCommentRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.CommentResponse;
import com.helpdeskpro.backend.security.CurrentUser;
import com.helpdeskpro.backend.security.UserPrincipal;
import com.helpdeskpro.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comment Controller
 * Handles all comment-related API endpoints
 */
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Comment management endpoints for tickets")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final CommentService commentService;

    /**
     * Create a new comment on a ticket
     *
     * POST /api/v1/tickets/{ticketId}/comments
     */
    @PostMapping
    @Operation(
            summary = "Add comment to ticket",
            description = "Create a new comment on a ticket. Only ticket creator, assigned agent, or admins can comment."
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} adding comment to ticket {}", currentUser.getId(), ticketId);

        CommentResponse comment = commentService.createComment(ticketId, request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(comment, "Comment added successfully"));
    }

    /**
     * Get all comments for a ticket
     *
     * GET /api/v1/tickets/{ticketId}/comments
     */
    @GetMapping
    @Operation(
            summary = "Get ticket comments",
            description = "Retrieve all comments for a ticket in chronological order (oldest first)"
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long ticketId,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} fetching comments for ticket {}", currentUser.getId(), ticketId);

        List<CommentResponse> comments = commentService.getCommentsByTicketId(ticketId, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        comments,
                        String.format("Found %d comments", comments.size())
                )
        );
    }

    /**
     * Update a comment
     *
     * PUT /api/v1/tickets/{ticketId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    @Operation(
            summary = "Update comment",
            description = "Update a comment. Only the comment creator can update their own comment."
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} updating comment {} on ticket {}",
                currentUser.getId(), commentId, ticketId);

        CommentResponse comment = commentService.updateComment(commentId, request, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(comment, "Comment updated successfully")
        );
    }

    /**
     * Delete a comment
     *
     * DELETE /api/v1/tickets/{ticketId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "Delete comment",
            description = "Soft delete a comment. Only the comment creator or admin can delete."
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} deleting comment {} on ticket {}",
                currentUser.getId(), commentId, ticketId);

        commentService.deleteComment(commentId, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(null, "Comment deleted successfully")
        );
    }

    /**
     * Get comment count for a ticket
     *
     * GET /api/v1/tickets/{ticketId}/comments/count
     */
    @GetMapping("/count")
    @Operation(
            summary = "Get comment count",
            description = "Get the total number of comments on a ticket"
    )
    public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable Long ticketId) {
        log.info("Fetching comment count for ticket {}", ticketId);

        Long count = commentService.getCommentCount(ticketId);

        return ResponseEntity.ok(
                ApiResponse.success(count, String.format("Ticket has %d comments", count))
        );
    }
}