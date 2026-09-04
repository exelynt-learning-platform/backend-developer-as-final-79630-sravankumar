package com.example.resourcebooking.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginationSortingAndFilteringTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Pagination - Default page=0, size=10")
    void testDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.content.length()", is(10)));
    }

    @Test
    @DisplayName("Pagination - Custom page and size")
    void testCustomPagination() throws Exception {
        mockMvc.perform(get("/api/resources?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.content.length()", is(5)));
    }

    @Test
    @DisplayName("Pagination - Protect against size exceeding 100 with 400 Bad Request")
    void testMaxPageSizeProtection() throws Exception {
        mockMvc.perform(get("/api/resources?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot exceed maximum limit of 100")));
    }

    @Test
    @DisplayName("Sorting - Valid sorting asc and desc")
    void testValidSorting() throws Exception {
        mockMvc.perform(get("/api/resources?sortBy=pricePerHour&sortDir=desc"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resources?sortBy=capacity&sortDir=asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sorting - Reject invalid unsafe sort field with 400 Bad Request")
    void testInvalidSortFieldRejected() throws Exception {
        mockMvc.perform(get("/api/resources?sortBy=maliciousField;DROP TABLE users;"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Filtering - Filter reservations by status, price range, and combined")
    void testReservationFiltering() throws Exception {
        // Filter by status
        mockMvc.perform(get("/api/admin/reservations?status=CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.notNullValue()));

        // Filter by minPrice
        mockMvc.perform(get("/api/admin/reservations?minPrice=500"))
                .andExpect(status().isOk());

        // Filter by maxPrice
        mockMvc.perform(get("/api/admin/reservations?maxPrice=5000"))
                .andExpect(status().isOk());

        // Combined filtering
        mockMvc.perform(get("/api/admin/reservations?status=CONFIRMED&minPrice=100&maxPrice=10000&page=0&size=5"))
                .andExpect(status().isOk());
    }
}
