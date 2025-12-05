package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.UpdateProfileRequest;
import com.helpdeskpro.backend.dto.request.UpdateUserRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.UserResponse;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.security.CurrentUser;
import com.helpdeskpro.backend.security.UserPrincipal;
import com.helpdeskpro.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    /**
     * Get current user's profile
     * Any authenticated user can access their own profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(@CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved successfully"));
    }

    /**
     * Update current user's profile
     * Any authenticated user can update their own profile
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    /**
     * Get user by ID
     * Admin can view any user, others can only view their own profile
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Get a specific user's details (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully"));
    }

    /**
     * Get all users
     * Only admins can view all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve a list of all users (Admin only)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved successfully"));
    }

    /**
     * Get users by role
     * Admins and Agents can view users by role
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get users by role", description = "Retrieve users filtered by their role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserRole role) {
        List<UserResponse> response = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved successfully"));
    }

    /**
     * Get all agents
     * Convenience endpoint for getting agents
     */
    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all agents", description = "Retrieve all users with AGENT role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAgents() {
        List<UserResponse> response = userService.getUsersByRole(UserRole.AGENT);
        return ResponseEntity.ok(ApiResponse.success(response, "Agents retrieved successfully"));
    }

    /**
     * Get all customers
     * Convenience endpoint for getting customers
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all customers", description = "Retrieve all users with CUSTOMER role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCustomers() {
        List<UserResponse> response = userService.getUsersByRole(UserRole.CUSTOMER);
        return ResponseEntity.ok(ApiResponse.success(response, "Customers retrieved successfully"));
    }

    /**
     * Update user (Admin only)
     * Allows admin to update any user's information
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update a user's information (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    /**
     * Delete user (Admin only)
     * Soft deletes a user from the system
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user from the system (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) { // <-- CHANGE: Return type is now ResponseEntity<Void>
        userService.deleteUser(id);
        return ResponseEntity.noContent().build(); // <-- CHANGE: Returns 204 No Content
    }
}