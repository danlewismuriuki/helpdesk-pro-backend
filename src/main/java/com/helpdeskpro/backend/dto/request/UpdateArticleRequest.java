package com.helpdeskpro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an article")
public class UpdateArticleRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(description = "Article title")
    private String title;

    @Size(min = 50, message = "Content must be at least 50 characters")
    @Schema(description = "Article content")
    private String content;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Brief summary")
    private String summary;

    @Schema(description = "Category slug")
    private String categorySlug;

    @Schema(description = "Article tags")
    private Set<String> tags;

    @Schema(description = "Published status")
    private Boolean published;
}
