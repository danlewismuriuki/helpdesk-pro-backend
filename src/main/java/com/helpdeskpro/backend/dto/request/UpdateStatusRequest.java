package com.helpdeskpro.backend.dto.request;

import com.helpdeskpro.backend.entity.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update ticket status")
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(
            description = "New ticket status (must follow workflow: OPEN → IN_PROGRESS → RESOLVED → CLOSED)",
            example = "IN_PROGRESS"
    )
    private TicketStatus status;
}