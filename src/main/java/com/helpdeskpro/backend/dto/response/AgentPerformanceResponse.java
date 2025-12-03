package com.helpdeskpro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent performance metrics")
public class AgentPerformanceResponse {

    @Schema(description = "Agent ID", example = "2")
    private Long agentId;

    @Schema(description = "Agent name", example = "John Doe")
    private String agentName;

    @Schema(description = "Agent email", example = "john@example.com")
    private String agentEmail;

    @Schema(description = "Number of tickets resolved", example = "45")
    private Long resolvedCount;

    @Schema(description = "Number of tickets currently assigned", example = "12")
    private Long assignedCount;

    @Schema(description = "Average resolution time in hours", example = "18.5")
    private Double averageResolutionTime;
}
