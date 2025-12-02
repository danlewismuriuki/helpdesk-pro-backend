package com.helpdeskpro.backend.security;

import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.exception.ResourceNotFoundException;
import com.helpdeskpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation
 * Loads user details for Spring Security authentication
 *
 * NOTE: Method name is "loadUserByUsername" but we're actually using EMAIL
 * This is a Spring Security requirement - the method name cannot be changed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email (called "username" by Spring Security convention)
     * This method is called during login authentication
     *
     * @param email - The user's email address (Spring calls it "username")
     * @return UserPrincipal - Our custom UserDetails implementation
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        log.debug("User found: {} (ID: {}, Role: {})", user.getEmail(), user.getId(), user.getRole());

        return UserPrincipal.create(user);
    }

    /**
     * Load user by ID
     * Used by JwtAuthenticationFilter when validating JWT tokens
     *
     * @param id - The user's database ID
     * @return UserPrincipal - Our custom UserDetails implementation
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long id) {
        log.debug("Loading user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        log.debug("User found: {} (Email: {}, Role: {})", user.getId(), user.getEmail(), user.getRole());

        return UserPrincipal.create(user);
    }
}