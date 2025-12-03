package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.CreateArticleRequest;
import com.helpdeskpro.backend.dto.request.UpdateArticleRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.ArticleResponse;
import com.helpdeskpro.backend.dto.response.ArticleSummaryResponse;
import com.helpdeskpro.backend.security.CurrentUser;
import com.helpdeskpro.backend.security.UserPrincipal;
import com.helpdeskpro.backend.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kb/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Knowledge Base - Articles", description = "Article management for knowledge base")
@SecurityRequirement(name = "Bearer Authentication")
public class ArticleController {

    private final ArticleService articleService;

    // ==================== PUBLIC ENDPOINTS (No auth required) ====================

    @GetMapping("/public")
    @Operation(summary = "Get all published articles", description = "Retrieve all published articles (public access)")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> getPublishedArticles(
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.getAllPublishedArticles(pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/public/{slug}")
    @Operation(summary = "Get article by slug", description = "Retrieve a specific article (increments view count)")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(@PathVariable String slug) {
        ArticleResponse article = articleService.getArticleBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @GetMapping("/public/search")
    @Operation(summary = "Search articles", description = "Search articles by keyword in title, content, or summary")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> searchArticles(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.searchArticles(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/public/category/{categorySlug}")
    @Operation(summary = "Get articles by category", description = "Retrieve articles in a specific category")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> getArticlesByCategory(
            @PathVariable String categorySlug,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.getArticlesByCategory(categorySlug, pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/public/tag/{tag}")
    @Operation(summary = "Get articles by tag", description = "Retrieve articles with a specific tag")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> getArticlesByTag(
            @PathVariable String tag,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.getArticlesByTag(tag, pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/public/most-viewed")
    @Operation(summary = "Get most viewed articles", description = "Retrieve articles sorted by view count")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> getMostViewed(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.getMostViewedArticles(pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/public/most-helpful")
    @Operation(summary = "Get most helpful articles", description = "Retrieve articles sorted by helpfulness")
    public ResponseEntity<ApiResponse<Page<ArticleSummaryResponse>>> getMostHelpful(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ArticleSummaryResponse> articles = articleService.getMostHelpfulArticles(pageable);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    // ==================== VOTING ENDPOINTS ====================

    @PostMapping("/public/{slug}/vote/helpful")
    @Operation(summary = "Vote article as helpful", description = "Mark article as helpful")
    public ResponseEntity<ApiResponse<ArticleResponse>> voteHelpful(@PathVariable String slug) {
        ArticleResponse article = articleService.voteHelpful(slug);
        return ResponseEntity.ok(ApiResponse.success(article, "Thank you for your feedback!"));
    }

    @PostMapping("/public/{slug}/vote/not-helpful")
    @Operation(summary = "Vote article as not helpful", description = "Mark article as not helpful")
    public ResponseEntity<ApiResponse<ArticleResponse>> voteNotHelpful(@PathVariable String slug) {
        ArticleResponse article = articleService.voteNotHelpful(slug);
        return ResponseEntity.ok(ApiResponse.success(article, "Thank you for your feedback!"));
    }

    // ==================== MANAGEMENT ENDPOINTS (Admin/Agent only) ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create article", description = "Create a new knowledge base article")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody CreateArticleRequest request,
            @CurrentUser UserPrincipal currentUser) {

        ArticleResponse article = articleService.createArticle(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(article, "Article created successfully"));
    }

    @PutMapping("/{slug}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update article", description = "Update an existing article")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable String slug,
            @Valid @RequestBody UpdateArticleRequest request,
            @CurrentUser UserPrincipal currentUser) {

        ArticleResponse article = articleService.updateArticle(slug, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(article, "Article updated successfully"));
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete article", description = "Delete an article (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @PathVariable String slug,
            @CurrentUser UserPrincipal currentUser) {

        articleService.deleteArticle(slug, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Article deleted successfully"));
    }
}