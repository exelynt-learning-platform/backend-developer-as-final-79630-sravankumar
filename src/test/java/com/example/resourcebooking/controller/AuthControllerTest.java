package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.auth.LoginRequest;
import com.example.resourcebooking.dto.auth.RegisterRequest;
import com.example.resourcebooking.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/register - Should successfully register new user with role USER")
    void testRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "newuser@example.com", "Password@123", Role.USER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/register - Should register user with ADMIN role if specified in request")
    void testRegisterAdminSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("customadmin", "customadmin@example.com", "Admin@123", Role.ADMIN);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("customadmin")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /auth/register - Should reject duplicate username with 400")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("admin", "anotheradmin@example.com", "Password@123", Role.ADMIN);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Username is already taken")));
    }

    @Test
    @DisplayName("POST /auth/register - Should reject invalid email with 400")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("bademailuser", "notanemail", "Password@123", Role.USER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()));
    }

    @Test
    @DisplayName("POST /auth/login - Should successfully authenticate seeded admin and return JWT")
    void testLoginAdminSuccess() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Admin@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("POST /auth/login - Should successfully authenticate seeded user1 and return JWT")
    void testLoginUserSuccess() throws Exception {
        LoginRequest request = new LoginRequest("user1", "User@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("user1")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("POST /auth/login - Should reject invalid password with 401")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("user1", "WrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /auth/login - Should reject unknown user with 401")
    void testLoginUnknownUser() throws Exception {
        LoginRequest request = new LoginRequest("unknownuser", "Password@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }
}
