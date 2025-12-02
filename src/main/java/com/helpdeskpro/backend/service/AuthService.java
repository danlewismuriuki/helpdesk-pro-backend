package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.request.LoginRequest;
import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.dto.response.AuthResponse;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.repository.UserRepository;
import com.helpdeskpro.backend.security.JwtUtil;
import com.helpdeskpro.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles user login and registration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Login user with email and password
     * Returns JWT token and user details
     */
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        // Step 1: Authenticate with Spring Security
        // This calls CustomUserDetailsService.loadUserByUsername(email)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),    // ← Using EMAIL as username
                        request.getPassword()
                )
        );

        // Step 2: Extract UserPrincipal from authentication
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Step 3: Generate JWT token with UserPrincipal
        String token = jwtUtil.generateToken(userPrincipal);

        // Step 4: Load full user details from database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.info("User logged in successfully: {} (ID: {})", user.getEmail(), user.getId());

        // Step 5: Return authentication response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Register new user
     * Auto-login after successful registration
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("User registration attempt: {}", request.getEmail());

        // Step 1: Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Step 2: Create new user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        // Step 3: Create UserPrincipal for auto-login
        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);

        // Step 4: Generate JWT token
        String token = jwtUtil.generateToken(userPrincipal);

        // Step 5: Return authentication response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }
}