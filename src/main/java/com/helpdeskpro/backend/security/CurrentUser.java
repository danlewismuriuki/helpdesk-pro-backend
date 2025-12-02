package com.helpdeskpro.backend.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * CurrentUser - Custom annotation to inject authenticated user
 *
 * Usage in controllers:
 * public ResponseEntity<?> someMethod(@CurrentUser UserPrincipal currentUser) {
 *     Long userId = currentUser.getId();
 *     String email = currentUser.getEmail();
 *     // ...
 * }
 *
 * This is a cleaner alternative to:
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * UserPrincipal currentUser = (UserPrincipal) auth.getPrincipal();
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}