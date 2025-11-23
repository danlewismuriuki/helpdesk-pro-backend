package com.helpdeskpro.backend.dto.response;

import com.helpdeskpro.backend.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    
    private Long id;
    private String username;
    private String email;
    private UserRole role;
}
