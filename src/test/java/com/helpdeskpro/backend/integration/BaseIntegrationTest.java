//package com.helpdeskpro.backend.integration;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.helpdeskpro.backend.dto.request.LoginRequest;
//import com.helpdeskpro.backend.dto.request.RegisterRequest;
//import com.helpdeskpro.backend.dto.response.AuthResponse;
//import com.helpdeskpro.backend.entity.enums.UserRole;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.transaction.annotation.Transactional;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//public abstract class BaseIntegrationTest {
//    @Autowired
//    protected MockMvc mockMvc;
//    @Autowired
//    protected ObjectMapper objectMapper;
//    protected String customerToken;
//    protected String agentToken;
//    protected String adminToken;
//
//    @BeforeEach
//    void baseSetUp() throws Exception {
//        // Register and login as customer
//        registerUser("customer@test.com", "Customer", "User", UserRole.CUSTOMER);
//        customerToken = loginUser("customer@test.com", "password123");
//        // Register and login as agent
//        registerUser("agent@test.com", "Agent", "User", UserRole.AGENT);
//        agentToken = loginUser("agent@test.com", "password123");
//        // Register and login as admin
//        registerUser("admin@test.com", "Admin", "User", UserRole.ADMIN);
//        adminToken = loginUser("admin@test.com", "password123");
//    }
//
//    protected void registerUser(String email, String firstName, String lastName, UserRole role) throws Exception {
//        RegisterRequest request = RegisterRequest.builder()
//                .username(email.split("@")[0])
//                .email(email)
//                .password("password123")
//                .firstName(firstName)
//                .lastName(lastName)
//                .role(role)
//                .build();
//        mockMvc.perform(post("/api/v1/auth/register")
//                        .with(csrf())  // Added CSRF token
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated());
//    }
//
//    protected String loginUser(String email, String password) throws Exception {
//        LoginRequest request = LoginRequest.builder()
//                .email(email)
//                .password(password)
//                .build();
//        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
//                        .with(csrf())  // Added CSRF token
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andReturn();
//        String responseBody = result.getResponse().getContentAsString();
//        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
//        return authResponse.getToken();
//    }
//
//    protected String asJsonString(Object obj) {
//        try {
//            return objectMapper.writeValueAsString(obj);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}


package com.helpdeskpro.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdeskpro.backend.dto.request.LoginRequest;
import com.helpdeskpro.backend.dto.request.RegisterRequest;
import com.helpdeskpro.backend.dto.response.AuthResponse;
import com.helpdeskpro.backend.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    protected String customerToken;
    protected String agentToken;
    protected String adminToken;

    @BeforeEach
    void baseSetUp() throws Exception {
        // Register and login as customer
        registerUser("customer@test.com", "Customer", "User", UserRole.CUSTOMER);
        customerToken = loginUser("customer@test.com", "password123");
        // Register and login as agent
        registerUser("agent@test.com", "Agent", "User", UserRole.AGENT);
        agentToken = loginUser("agent@test.com", "password123");
        // Register and login as admin
        registerUser("admin@test.com", "Admin", "User", UserRole.ADMIN);
        adminToken = loginUser("admin@test.com", "password123");
    }

    protected void registerUser(String email, String firstName, String lastName, UserRole role) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username(email.split("@")[0])
                .email(email)
                .password("password123")
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    protected String loginUser(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        // .with(csrf()) // <-- REMOVED CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();

        // Correctly parse the wrapped response
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode dataNode = root.path("data");
        AuthResponse authResponse = objectMapper.treeToValue(dataNode, AuthResponse.class);

        return authResponse.getToken();
    }

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}