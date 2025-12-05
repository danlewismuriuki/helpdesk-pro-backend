package com.helpdeskpro.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints - PUBLIC
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Actuator & Swagger - PUBLIC
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()

                        // Public API
                        .requestMatchers("/api/public/**").permitAll()

                        // --- CRITICAL FIXES ---
                        // Specific rules for users to manage their own profiles MUST come first
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated()

                        // Specific rule for customers to get their own tickets
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/my-tickets").authenticated()

                        // Tickets & Comments - PROTECTED
                        .requestMatchers("/api/v1/tickets/**").hasAnyRole("CUSTOMER", "AGENT", "ADMIN")
                        .requestMatchers("/api/v1/comments/**").hasAnyRole("CUSTOMER", "AGENT", "ADMIN")

                        // Users - PROTECTED (for admin/agent management)
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "AGENT")

                        // Agent endpoints
                        .requestMatchers("/api/v1/agent/**").hasAnyRole("AGENT", "ADMIN")

                        // Admin endpoints - PROTECTED
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                            Map<String, Object> errorDetails = new HashMap<>();
                            errorDetails.put("timestamp", LocalDateTime.now().toString());
                            errorDetails.put("status", 401);
                            errorDetails.put("error", "Unauthorized");
                            errorDetails.put("message", "Authentication required");
                            errorDetails.put("path", request.getRequestURI());

                            ObjectMapper mapper = new ObjectMapper();
                            response.getWriter().write(mapper.writeValueAsString(errorDetails));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            Map<String, Object> errorDetails = new HashMap<>();
                            errorDetails.put("timestamp", LocalDateTime.now().toString());
                            errorDetails.put("status", 403);
                            errorDetails.put("error", "Forbidden");
                            errorDetails.put("message", "Access denied");
                            errorDetails.put("path", request.getRequestURI());

                            ObjectMapper mapper = new ObjectMapper();
                            response.getWriter().write(mapper.writeValueAsString(errorDetails));
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}