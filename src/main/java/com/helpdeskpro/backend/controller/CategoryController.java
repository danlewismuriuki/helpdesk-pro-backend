package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.CreateCategoryRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.CategoryResponse;
import com.helpdeskpro.backend.security.CurrentUser;
import com.helpdeskpro.backend.security.UserPrincipal;
import com.helpdeskpro.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kb/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Knowledge Base - Categories", description = "Category management for knowledge base")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Create a new category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            @CurrentUser UserPrincipal currentUser) {

        CategoryResponse category = categoryService.createCategory(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve a specific category")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PutMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Update a category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable String slug,
            @Valid @RequestBody CreateCategoryRequest request,
            @CurrentUser UserPrincipal currentUser) {

        CategoryResponse category = categoryService.updateCategory(slug, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Delete a category (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String slug,
            @CurrentUser UserPrincipal currentUser) {

        categoryService.deleteCategory(slug, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }
}