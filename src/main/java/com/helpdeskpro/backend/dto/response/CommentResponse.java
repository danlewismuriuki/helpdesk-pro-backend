package com.helpdeskpro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comment response with user and timestamp information")
public class CommentResponse {

    @Schema(description = "Comment ID", example = "1")
    private Long id;

    @Schema(description = "Comment content", example = "I've tried resetting my password...")
    private String content;

    @Schema(description = "Ticket ID this comment belongs to", example = "5")
    private Long ticketId;

    @Schema(description = "User who created the comment")
    private UserResponse createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Comment creation timestamp", example = "2024-12-02T14:30:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Comment last update timestamp", example = "2024-12-02T15:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Whether the comment has been edited", example = "false")
    private Boolean isEdited;
}