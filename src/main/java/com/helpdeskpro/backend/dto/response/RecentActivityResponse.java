package com.helpdeskpro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recent activity entry")
public class RecentActivityResponse {

    @Schema(description = "Activity type", example = "TICKET_CREATED")
    private String activityType;

    @Schema(description = "Activity description", example = "New ticket created: Cannot login")
    private String description;

    @Schema(description = "User who performed the action", example = "John Doe")
    private String userName;

    @Schema(description = "Related entity ID", example = "45")
    private Long entityId;

    @Schema(description = "Related entity type", example = "TICKET")
    private String entityType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Activity timestamp")
    private LocalDateTime timestamp;
}
