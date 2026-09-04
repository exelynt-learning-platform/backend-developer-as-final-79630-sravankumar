package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.reservation.ReservationCreateRequest;
import com.example.resourcebooking.dto.reservation.ReservationResponse;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.ResourceType;
import com.example.resourcebooking.enums.Role;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;
import com.example.resourcebooking.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationOwnershipAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Resource testRoom;
    private ReservationResponse user1Reservation;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("user2")) {
            userRepository.save(new User("user2", "user2@example.com", passwordEncoder.encode("User@123"), Role.USER, true));
        }

        testRoom = resourceRepository.save(new Resource(
                "Ownership Room",
                "Description",
                ResourceType.ROOM,
                "Tower C",
                6,
                BigDecimal.valueOf(800.00),
                true
        ));

        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);
        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), start, end);
        user1Reservation = reservationService.createReservation(request, "user1");
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    @DisplayName("User1 should successfully access their own reservation")
    void testUserCanAccessOwnReservation() throws Exception {
        mockMvc.perform(get("/api/reservations/" + user1Reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user1Reservation.getId().intValue())))
                .andExpect(jsonPath("$.username", is("user1")));
    }

    @Test
    @WithMockUser(username = "user2", roles = {"USER"})
    @DisplayName("User2 attempting to access User1's reservation must receive 403 Forbidden")
    void testUserCannotAccessOtherUserReservation() throws Exception {
        mockMvc.perform(get("/api/reservations/" + user1Reservation.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @WithMockUser(username = "user2", roles = {"USER"})
    @DisplayName("User2 attempting to cancel User1's reservation must receive 403 Forbidden")
    void testUserCannotCancelOtherUserReservation() throws Exception {
        mockMvc.perform(delete("/api/reservations/" + user1Reservation.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin should be able to view any user's reservation")
    void testAdminCanViewAnyReservation() throws Exception {
        mockMvc.perform(get("/api/admin/reservations/" + user1Reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user1Reservation.getId().intValue())))
                .andExpect(jsonPath("$.username", is("user1")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    @DisplayName("User attempting to access /api/admin/reservations must receive 403 Forbidden")
    void testUserCannotAccessAdminReservationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/reservations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}
