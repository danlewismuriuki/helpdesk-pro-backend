package com.helpdeskpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Article Entity
 * Represents a knowledge base article
 * Features: categories, tags, view count, helpful voting
 */
@Entity
@Table(name = "articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 200)
    private String slug; // URL-friendly title

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String summary; // Brief description

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false; // Draft vs Published

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long helpfulCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long notHelpfulCount = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Increment view count
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Vote article as helpful
     */
    public void voteHelpful() {
        this.helpfulCount++;
    }

    /**
     * Vote article as not helpful
     */
    public void voteNotHelpful() {
        this.notHelpfulCount++;
    }

    /**
     * Publish the article
     */
    public void publish() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Unpublish the article (make it draft)
     */
    public void unpublish() {
        this.published = false;
        this.publishedAt = null;
    }

    /**
     * Soft delete the article
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Check if article is deleted
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Calculate helpfulness percentage
     */
    public double getHelpfulnessPercentage() {
        long total = this.helpfulCount + this.notHelpfulCount;
        if (total == 0) return 0.0;
        return (double) this.helpfulCount / total * 100;
    }
}