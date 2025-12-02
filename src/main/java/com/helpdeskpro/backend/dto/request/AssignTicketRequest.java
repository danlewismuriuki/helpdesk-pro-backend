package com.helpdeskpro.backend.dto.request;

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
@Schema(description = "Request to assign ticket to an agent")
public class AssignTicketRequest {

    @NotNull(message = "Agent ID is required")
    @Schema(description = "ID of the agent to assign the ticket to", example = "2")
    private Long agentId;
}