package com.helpdeskpro.backend.repository;

import com.helpdeskpro.backend.entity.Article;
import com.helpdeskpro.backend.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * Find article by slug
     */
    Optional<Article> findBySlug(String slug);

    /**
     * Find all published articles (non-deleted)
     */
    @Query("SELECT a FROM Article a WHERE a.published = true AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Article> findAllPublished(Pageable pageable);

    /**
     * Find published articles by category
     */
    @Query("SELECT a FROM Article a WHERE a.category = :category AND a.published = true AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Article> findByCategoryAndPublished(@Param("category") Category category, Pageable pageable);

    /**
     * Search articles by title or content (full-text search)
     */
    @Query("SELECT a FROM Article a WHERE a.published = true AND a.deletedAt IS NULL AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY a.viewCount DESC")
    Page<Article> searchPublishedArticles(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find articles by tag
     */
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t = :tag AND a.published = true AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Article> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Find most viewed articles
     */
    @Query("SELECT a FROM Article a WHERE a.published = true AND a.deletedAt IS NULL ORDER BY a.viewCount DESC")
    Page<Article> findMostViewed(Pageable pageable);

    /**
     * Find most helpful articles
     */
    @Query("SELECT a FROM Article a WHERE a.published = true AND a.deletedAt IS NULL ORDER BY a.helpfulCount DESC")
    Page<Article> findMostHelpful(Pageable pageable);

    /**
     * Find all articles (including drafts) - Admin only
     */
    @Query("SELECT a FROM Article a WHERE a.deletedAt IS NULL ORDER BY a.updatedAt DESC")
    Page<Article> findAllIncludingDrafts(Pageable pageable);

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Count published articles
     */
    @Query("SELECT COUNT(a) FROM Article a WHERE a.published = true AND a.deletedAt IS NULL")
    Long countPublished();

    /**
     * Count articles by category
     */
    @Query("SELECT COUNT(a) FROM Article a WHERE a.category = :category AND a.published = true AND a.deletedAt IS NULL")
    Long countByCategory(@Param("category") Category category);
}