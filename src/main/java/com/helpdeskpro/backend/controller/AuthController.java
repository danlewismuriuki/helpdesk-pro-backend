//package com.helpdeskpro.backend.controller;
//
//import com.helpdeskpro.backend.dto.request.LoginRequest;
//import com.helpdeskpro.backend.dto.request.RegisterRequest;
//import com.helpdeskpro.backend.dto.response.AuthResponse;
//import com.helpdeskpro.backend.service.AuthService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/login")
//    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
//        AuthResponse response = authService.login(request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/register")
//    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
//        AuthResponse response = authService.register(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//}



package com.helpdeskpro.backend.controller;

import com.helpdeskpro.backend.dto.request.LoginRequest;
import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.dto.response.ApiResponse;
import com.helpdeskpro.backend.dto.response.AuthResponse;
import com.helpdeskpro.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }
}
