package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.resource.ResourceCreateRequest;
import com.example.resourcebooking.dto.resource.ResourceUpdateRequest;
import com.example.resourcebooking.enums.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/resources - Public access should return seeded rooms with 200 OK")
    void testPublicGetResources() throws Exception {
        mockMvc.perform(get("/api/resources?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.content.length()", is(10)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/resources - ADMIN should successfully create resource")
    void testAdminCreateResource() throws Exception {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Training Hall A",
                "Large training room with projectors",
                ResourceType.ROOM,
                "Building 3, Floor 4",
                30,
                BigDecimal.valueOf(2500.00),
                true
        );

        mockMvc.perform(post("/api/admin/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Training Hall A")))
                .andExpect(jsonPath("$.capacity", is(30)))
                .andExpect(jsonPath("$.pricePerHour", is(2500.00)));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    @DisplayName("POST /api/admin/resources - USER should receive 403 Forbidden")
    void testUserCannotCreateResource() throws Exception {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Illegal Room",
                "Description",
                ResourceType.ROOM,
                "Floor 1",
                10,
                BigDecimal.valueOf(500.00),
                true
        );

        mockMvc.perform(post("/api/admin/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("POST /api/admin/resources - Unauthenticated request should receive 401 Unauthorized")
    void testUnauthenticatedCannotCreateResource() throws Exception {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Anonymous Room",
                "Description",
                ResourceType.ROOM,
                "Floor 1",
                10,
                BigDecimal.valueOf(500.00),
                true
        );

        mockMvc.perform(post("/api/admin/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/resources - Should return 400 when validation fails")
    void testValidationFailureOnCreate() throws Exception {
        ResourceCreateRequest invalidRequest = new ResourceCreateRequest(
                "", // blank name
                "Description",
                ResourceType.ROOM,
                "Floor 1",
                -5, // invalid capacity
                BigDecimal.valueOf(-100.00), // invalid price
                true
        );

        mockMvc.perform(post("/api/admin/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name", is("Resource name is required")))
                .andExpect(jsonPath("$.fieldErrors.capacity", is("Capacity must be greater than zero")))
                .andExpect(jsonPath("$.fieldErrors.pricePerHour", is("Price per hour must be greater than zero")));
    }
}
