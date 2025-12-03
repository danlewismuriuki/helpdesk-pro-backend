package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.CreateCategoryRequest;
import com.helpdeskpro.backend.dto.response.CategoryResponse;
import com.helpdeskpro.backend.entity.Category;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.exception.InvalidOperationException;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.ArticleRepository;
import com.helpdeskpro.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Category Service
 * Handles category management for knowledge base
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final UserService userService;

    /**
     * Create a new category
     * Business Rule: Only ADMIN can create categories
     */
    public CategoryResponse createCategory(CreateCategoryRequest request, Long userId) {
        log.info("Creating category: {}", request.getName());

        User user = userService.getUserEntityById(userId);

        // Only admins can create categories
        if (user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("Only admins can create categories");
        }

        // Check if category name already exists
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new InvalidOperationException("Category with name '" + request.getName() + "' already exists");
        }

        // Generate slug
        String slug = generateSlug(request.getName());

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slug(slug)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully: {}", savedCategory.getName());

        return mapToResponse(savedCategory);
    }

    /**
     * Get all active categories
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get category by slug
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToResponse(category);
    }

    /**
     * Update category
     * Business Rule: Only ADMIN can update categories
     */
    public CategoryResponse updateCategory(String slug, CreateCategoryRequest request, Long userId) {
        log.info("Updating category: {}", slug);

        User user = userService.getUserEntityById(userId);

        // Only admins can update categories
        if (user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("Only admins can update categories");
        }

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        // Update fields
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            // Check if new name already exists
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                throw new InvalidOperationException("Category with name '" + request.getName() + "' already exists");
            }
            category.setName(request.getName());
            category.setSlug(generateSlug(request.getName()));
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", slug);

        return mapToResponse(updatedCategory);
    }

    /**
     * Delete category (soft delete)
     * Business Rule: Only ADMIN can delete categories
     */
    public void deleteCategory(String slug, Long userId) {
        log.info("Deleting category: {}", slug);

        User user = userService.getUserEntityById(userId);

        // Only admins can delete categories
        if (user.getRole() != UserRole.ADMIN) {
            throw new InvalidOperationException("Only admins can delete categories");
        }

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        // Check if category has articles
        Long articleCount = articleRepository.countByCategory(category);
        if (articleCount > 0) {
            throw new InvalidOperationException(
                    "Cannot delete category with " + articleCount + " articles. " +
                            "Please move or delete articles first."
            );
        }

        category.softDelete();
        categoryRepository.save(category);

        log.info("Category deleted successfully: {}", slug);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate URL-friendly slug from name
     */
    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // Ensure uniqueness
        String slug = baseSlug;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    /**
     * Map Category to CategoryResponse
     */
    private CategoryResponse mapToResponse(Category category) {
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