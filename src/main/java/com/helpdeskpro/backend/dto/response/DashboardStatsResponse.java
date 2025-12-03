package com.helpdeskpro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete dashboard statistics")
public class DashboardStatsResponse {

    @Schema(description = "Total number of tickets", example = "150")
    private Long totalTickets;

    @Schema(description = "Tickets grouped by status")
    private Map<String, Long> ticketsByStatus;

    @Schema(description = "Tickets grouped by priority")
    private Map<String, Long> ticketsByPriority;

    @Schema(description = "Average ticket resolution time in hours", example = "24.5")
    private Double averageResolutionTimeHours;

    @Schema(description = "SLA breach rate percentage", example = "12.3")
    private Double slaBreachRate;

    @Schema(description = "Number of tickets breaching SLA", example = "8")
    private Long slaBreachedTickets;

    @Schema(description = "Top performing agents by resolved tickets")
    private List<AgentPerformanceResponse> topAgents;

    @Schema(description = "Knowledge base statistics")
    private KnowledgeBaseStatsResponse knowledgeBaseStats;

    @Schema(description = "Recent ticket activity")
    private List<RecentActivityResponse> recentActivity;
}