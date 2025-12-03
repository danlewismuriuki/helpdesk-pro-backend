package com.helpdeskpro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request to create an article")
public class CreateArticleRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(description = "Article title", example = "How to reset your password")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 50, message = "Content must be at least 50 characters")
    @Schema(description = "Article content in markdown or HTML", example = "## Steps to reset password\n1. Click forgot password...")
    private String content;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Brief summary", example = "Learn how to reset your account password in 3 easy steps")
    private String summary;

    @Schema(description = "Category slug", example = "getting-started")
    private String categorySlug;

    @Schema(description = "Article tags", example = "[\"password\", \"account\", \"security\"]")
    private Set<String> tags;

    @Schema(description = "Publish immediately or save as draft", example = "false")
    private Boolean published;
}