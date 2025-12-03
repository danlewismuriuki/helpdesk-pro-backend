package com.helpdeskpro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article response")
public class ArticleResponse {

    @Schema(description = "Article ID", example = "1")
    private Long id;

    @Schema(description = "Article title", example = "How to reset your password")
    private String title;

    @Schema(description = "URL-friendly slug", example = "how-to-reset-your-password")
    private String slug;

    @Schema(description = "Article content")
    private String content;

    @Schema(description = "Brief summary")
    private String summary;

    @Schema(description = "Category information")
    private CategoryResponse category;

    @Schema(description = "Article tags", example = "[\"password\", \"account\"]")
    private Set<String> tags;

    @Schema(description = "Author information")
    private UserResponse createdBy;

    @Schema(description = "Published status", example = "true")
    private Boolean published;

    @Schema(description = "View count", example = "342")
    private Long viewCount;

    @Schema(description = "Helpful votes", example = "45")
    private Long helpfulCount;

    @Schema(description = "Not helpful votes", example = "5")
    private Long notHelpfulCount;

    @Schema(description = "Helpfulness percentage", example = "90.0")
    private Double helpfulnessPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Publication timestamp")
    private LocalDateTime publishedAt;
}
