package com.helpdeskpro.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility Class
 * Handles JWT token generation, validation, and claim extraction
 *
 * Token Structure:
 * {
 *   "sub": "123",                    // User ID (primary identifier)
 *   "email": "john@example.com",     // User email (for reference)
 *   "role": "ADMIN",                 // User role
 *   "iat": 1638360000,               // Issued at
 *   "exp": 1638446400                // Expiration
 * }
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Get signing key for JWT
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ========================================================================
    // TOKEN GENERATION (ID-Based)
    // ========================================================================

    /**
     * Generate JWT token with UserPrincipal (ID-based)
     * This is the MAIN method used throughout the application
     */
    public String generateToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", userPrincipal.getEmail());
        claims.put("role", userPrincipal.getRole().name());

        // Store USER ID as subject (primary identifier)
        return createToken(claims, String.valueOf(userPrincipal.getId()));
    }

    /**
     * Generate token from UserDetails (backward compatibility)
     * Used during login when we have UserDetails from authentication
     */
    public String generateToken(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal) {
            return generateToken((UserPrincipal) userDetails);
        }

        // Fallback: use email as subject (less optimal)
        Map<String, Object> claims = new HashMap<>();
        log.warn("Generating token from UserDetails (not UserPrincipal) - using email as subject");
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // ========================================================================
    // CLAIM EXTRACTION
    // ========================================================================

    /**
     * Extract user ID from JWT token (PRIMARY METHOD)
     * Returns the user's database ID
     */
    public Long extractUserId(String token) {
        String idString = extractClaim(token, Claims::getSubject);
        try {
            return Long.parseLong(idString);
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID from token subject: {}", idString);
            throw new IllegalArgumentException("Invalid token: subject is not a valid user ID");
        }
    }

    /**
     * Extract username (email) from token subject
     * DEPRECATED: Use extractUserId() instead
     * Kept for backward compatibility
     */
    @Deprecated
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract email from token claims
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract role from token claims
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim using a resolver function
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ========================================================================
    // TOKEN VALIDATION
    // ========================================================================

    /**
     * Validate token against UserPrincipal (ID-based validation)
     * This is the MAIN validation method
     */
    public Boolean validateToken(String token, UserPrincipal userPrincipal) {
        try {
            final Long userId = extractUserId(token);
            return (userId.equals(userPrincipal.getId()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token against UserDetails (backward compatibility)
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal) {
            return validateToken(token, (UserPrincipal) userDetails);
        }

        // Fallback: validate by username (email)
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token structure and signature (without user validation)
     */
    public Boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Invalid token structure: {}", e.getMessage());
            return false;
        }
    }
}