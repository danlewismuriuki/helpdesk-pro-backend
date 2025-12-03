package com.helpdeskpro.backend.repository;

import com.helpdeskpro.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find category by slug
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find all non-deleted categories
     */
    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findAllActive();

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Find category by name
     */
    Optional<Category> findByName(String name);
}