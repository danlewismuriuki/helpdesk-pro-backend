package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.dto.response.UserResponse;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - User Management Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();
    }

    // ==================== CREATE USER TESTS ====================

    @Test
    @DisplayName("Should create user successfully")
    void createUser_WithValidData_ShouldReturnUserResponse() {
        // Given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.createUser(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username exists")
    void createUser_WithExistingUsername_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    // ==================== GET USER TESTS ====================

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WhenUserExists_ShouldReturnUserResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get user entity by ID")
    void getUserEntityById_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User user = userService.getUserEntityById(1L);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUser.getId());
        assertThat(user.getEmail()).isEqualTo(testUser.getEmail());
    }

    // ==================== GET ALL USERS TESTS ====================

    @Test
    @DisplayName("Should get all users")
    void getAllUsers_ShouldReturnListOfUsers() {
        // Given
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .role(UserRole.AGENT)
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        // When
        List<UserResponse> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getUsername()).isEqualTo("testuser");
        assertThat(users.get(1).getUsername()).isEqualTo("user2");

        verify(userRepository).findAll();
    }

    // ==================== GET USERS BY ROLE TESTS ====================

    @Test
    @DisplayName("Should get users by role")
    void getUsersByRole_ShouldReturnFilteredUsers() {
        // Given
        User agent1 = User.builder()
                .id(2L)
                .username("agent1")
                .email("agent1@example.com")
                .role(UserRole.AGENT)
                .build();

        User agent2 = User.builder()
                .id(3L)
                .username("agent2")
                .email("agent2@example.com")
                .role(UserRole.AGENT)
                .build();

        when(userRepository.findByRole(UserRole.AGENT))
                .thenReturn(Arrays.asList(agent1, agent2));

        // When
        List<UserResponse> agents = userService.getUsersByRole(UserRole.AGENT);

        // Then
        assertThat(agents).hasSize(2);
        assertThat(agents).allMatch(user -> user.getRole() == UserRole.AGENT);

        verify(userRepository).findByRole(UserRole.AGENT);
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    @DisplayName("Should soft delete user")
    void deleteUser_ShouldSetDeletedAt() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void deleteUser_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}