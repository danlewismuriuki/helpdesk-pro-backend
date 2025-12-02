package com.helpdeskpro.backend.dto.request;

import com.helpdeskpro.backend.entity.enums.TicketPriority;
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
@Schema(description = "Request to update ticket priority")
public class UpdatePriorityRequest {

    @NotNull(message = "Priority is required")
    @Schema(description = "New priority level", example = "HIGH")
    private TicketPriority priority;
}