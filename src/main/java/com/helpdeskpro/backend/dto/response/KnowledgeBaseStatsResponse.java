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
@Schema(description = "Knowledge base statistics")
public class KnowledgeBaseStatsResponse {

    @Schema(description = "Total published articles", example = "25")
    private Long totalArticles;

    @Schema(description = "Total draft articles", example = "5")
    private Long draftArticles;

    @Schema(description = "Total article views", example = "1500")
    private Long totalViews;

    @Schema(description = "Average helpfulness percentage", example = "87.5")
    private Double averageHelpfulness;

    @Schema(description = "Most viewed article")
    private ArticleSummaryResponse mostViewedArticle;
}
