package com.helpdeskpro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Lightweight article response for listings
 * Does not include full content
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article summary for listings")
public class ArticleSummaryResponse {

    @Schema(description = "Article ID")
    private Long id;

    @Schema(description = "Article title")
    private String title;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Brief summary")
    private String summary;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Article tags")
    private Set<String> tags;

    @Schema(description = "View count")
    private Long viewCount;

    @Schema(description = "Helpfulness percentage")
    private Double helpfulnessPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Publication timestamp")
    private LocalDateTime publishedAt;
}