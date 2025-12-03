package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.LoginRequest;
import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.dto.response.AuthResponse;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.repository.UserRepository;
import com.helpdeskpro.backend.security.JwtUtil;
import com.helpdeskpro.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Authentication Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private UserPrincipal userPrincipal;

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

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .build();

        userPrincipal = UserPrincipal.create(testUser);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getRole()).isEqualTo(testUser.getRole());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(any(UserPrincipal.class));
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }

    @Test
    @DisplayName("Should throw exception with invalid credentials")
    void login_WithInvalidCredentials_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void login_WhenUserNotFoundAfterAuth_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .hasMessageContaining("User not found");
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void register_WithValidData_ShouldCreateUserAndReturnAuthResponse() {
        // Given
        User newUser = User.builder()
                .id(2L)
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password("$2a$10$encodedPassword")
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(registerRequest.getRole())
                .active(true)
                .build();

        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo(registerRequest.getUsername());
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(any(UserPrincipal.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void register_WithExistingUsername_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should encode password during registration")
    void register_ShouldEncodePassword() {
        // Given
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$10$encodedPassword")
        ));
    }

    @Test
    @DisplayName("Should set user as active during registration")
    void register_ShouldSetUserActive() {
        // Given
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getActive() != null && user.getActive()
        ));
    }
}