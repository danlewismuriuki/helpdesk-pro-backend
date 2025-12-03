package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateArticleRequest;
import com.helpdeskpro.backend.dto.request.UpdateArticleRequest;
import com.helpdeskpro.backend.dto.response.ArticleResponse;
import com.helpdeskpro.backend.dto.response.ArticleSummaryResponse;
import com.helpdeskpro.backend.dto.response.CategoryResponse;
import com.helpdeskpro.backend.entity.Article;
import com.helpdeskpro.backend.entity.Category;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.ArticleRepository;
import com.helpdeskpro.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Article Service
 * Handles all knowledge base article operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    /**
     * Create a new article
     * Business Rule: Only ADMIN or AGENT can create articles
     */
    public ArticleResponse createArticle(CreateArticleRequest request, Long userId) {
        log.info("Creating article: {}", request.getTitle());

        User user = userService.getUserEntityById(userId);

        // Validate permission
        if (!canManageArticles(user)) {
            throw new InvalidOperationException("Only admins and agents can create articles");
        }

        // Get category if provided
        Category category = null;
        if (request.getCategorySlug() != null) {
            category = categoryRepository.findBySlug(request.getCategorySlug())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", request.getCategorySlug()));
        }

        // Generate unique slug
        String slug = generateSlug(request.getTitle());

        Article article = Article.builder()
                .title(request.getTitle())
                .slug(slug)
                .content(request.getContent())
                .summary(request.getSummary())
                .category(category)
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .createdBy(user)
                .published(request.getPublished() != null ? request.getPublished() : false)
                .build();

        if (article.getPublished()) {
            article.publish();
        }

        Article savedArticle = articleRepository.save(article);
        log.info("Article created successfully: ID {}", savedArticle.getId());

        return mapToFullResponse(savedArticle);
    }

    /**
     * Get article by slug (increments view count)
     */
    @Transactional
    public ArticleResponse getArticleBySlug(String slug) {
        log.info("Fetching article by slug: {}", slug);

        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        // Only show published articles to non-admins
        if (!article.getPublished()) {
            throw new ResourceNotFoundException("Article", "slug", slug);
        }

        // Increment view count
        article.incrementViewCount();
        articleRepository.save(article);

        return mapToFullResponse(article);
    }

    /**
     * Get all published articles
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getAllPublishedArticles(Pageable pageable) {
        return articleRepository.findAllPublished(pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Get articles by category
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getArticlesByCategory(String categorySlug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", categorySlug));

        return articleRepository.findByCategoryAndPublished(category, pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Search articles by keyword
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchArticles(String keyword, Pageable pageable) {
        log.info("Searching articles with keyword: {}", keyword);
        return articleRepository.searchPublishedArticles(keyword, pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Get articles by tag
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getArticlesByTag(String tag, Pageable pageable) {
        return articleRepository.findByTag(tag, pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Get most viewed articles
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getMostViewedArticles(Pageable pageable) {
        return articleRepository.findMostViewed(pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Get most helpful articles
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getMostHelpfulArticles(Pageable pageable) {
        return articleRepository.findMostHelpful(pageable)
                .map(this::mapToSummaryResponse);
    }

    /**
     * Update article
     * Business Rule: Only ADMIN or article creator can update
     */
    public ArticleResponse updateArticle(String slug, UpdateArticleRequest request, Long userId) {
        log.info("Updating article: {}", slug);

        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        User user = userService.getUserEntityById(userId);

        // Validate permission
        if (!canModifyArticle(user, article)) {
            throw new InvalidOperationException("You do not have permission to update this article");
        }

        // Update fields
        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
            article.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getCategorySlug() != null) {
            Category category = categoryRepository.findBySlug(request.getCategorySlug())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", request.getCategorySlug()));
            article.setCategory(category);
        }
        if (request.getTags() != null) {
            article.setTags(request.getTags());
        }
        if (request.getPublished() != null) {
            if (request.getPublished() && !article.getPublished()) {
                article.publish();
            } else if (!request.getPublished() && article.getPublished()) {
                article.unpublish();
            }
        }

        Article updatedArticle = articleRepository.save(article);
        log.info("Article updated successfully: {}", slug);

        return mapToFullResponse(updatedArticle);
    }

    /**
     * Delete article (soft delete)
     * Business Rule: Only ADMIN can delete
     */
    public void deleteArticle(String slug, Long userId) {
        log.info("Deleting article: {}", slug);

        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        User user = userService.getUserEntityById(userId);

        // Only admins can delete
        if (user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("Only admins can delete articles");
        }

        article.softDelete();
        articleRepository.save(article);

        log.info("Article deleted successfully: {}", slug);
    }

    /**
     * Vote article as helpful
     */
    @Transactional
    public ArticleResponse voteHelpful(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        article.voteHelpful();
        Article updatedArticle = articleRepository.save(article);

        log.info("Article {} voted as helpful (total: {})", slug, article.getHelpfulCount());

        return mapToFullResponse(updatedArticle);
    }

    /**
     * Vote article as not helpful
     */
    @Transactional
    public ArticleResponse voteNotHelpful(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        article.voteNotHelpful();
        Article updatedArticle = articleRepository.save(article);

        log.info("Article {} voted as not helpful (total: {})", slug, article.getNotHelpfulCount());

        return mapToFullResponse(updatedArticle);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Check if user can manage articles (create/edit)
     */
    private boolean canManageArticles(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AGENT;
    }

    /**
     * Check if user can modify specific article
     */
    private boolean canModifyArticle(User user, Article article) {
        // Admins can modify any article
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Article creator (agent) can modify their own article
        return article.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Generate URL-friendly slug from title
     */
    private String generateSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // Ensure uniqueness
        String slug = baseSlug;
        int counter = 1;
        while (articleRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    /**
     * Map Article to full ArticleResponse
     */
    private ArticleResponse mapToFullResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .summary(article.getSummary())
                .category(article.getCategory() != null ? mapCategoryToResponse(article.getCategory()) : null)
                .tags(article.getTags())
                .createdBy(userService.getUserById(article.getCreatedBy().getId()))
                .published(article.getPublished())
                .viewCount(article.getViewCount())
                .helpfulCount(article.getHelpfulCount())
                .notHelpfulCount(article.getNotHelpfulCount())
                .helpfulnessPercentage(article.getHelpfulnessPercentage())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .publishedAt(article.getPublishedAt())
                .build();
    }

    /**
     * Map Article to summary ArticleSummaryResponse (for listings)
     */
    private ArticleSummaryResponse mapToSummaryResponse(Article article) {
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .categoryName(article.getCategory() != null ? article.getCategory().getName() : null)
                .tags(article.getTags())
                .viewCount(article.getViewCount())
                .helpfulnessPercentage(article.getHelpfulnessPercentage())
                .publishedAt(article.getPublishedAt())
                .build();
    }

    /**
     * Map Category to CategoryResponse
     */
    private CategoryResponse mapCategoryToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .articleCount(articleRepository.countByCategory(category))
                .createdAt(category.getCreatedAt())
                .build();
    }
}