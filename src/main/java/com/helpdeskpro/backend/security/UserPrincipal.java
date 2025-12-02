package com.helpdeskpro.backend.security;

import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserPrincipal - Spring Security UserDetails implementation
 * Represents the authenticated user in the security context
 */
@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private UserRole role;
    private boolean active;

    /**
     * Factory method to create UserPrincipal from User entity
     */
    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getActive()
        );
    }

    /**
     * Returns the email as the username (modern approach)
     * This is what Spring Security uses for authentication
     */
    @Override
    public String getUsername() {
        return email;  // Using EMAIL as primary identifier
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns user authorities/roles
     * Format: ROLE_ADMIN, ROLE_AGENT, ROLE_USER
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}