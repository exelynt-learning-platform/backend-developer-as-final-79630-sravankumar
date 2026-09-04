package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.reservation.ReservationCreateRequest;
import com.example.resourcebooking.dto.reservation.ReservationResponse;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.enums.ReservationStatus;
import com.example.resourcebooking.enums.ResourceType;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationConflictException;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ReservationServiceAndConflictTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Resource testRoom;

    @BeforeEach
    void setUp() {
        testRoom = resourceRepository.save(new Resource(
                "Conflict Test Room",
                "Description",
                ResourceType.ROOM,
                "Floor 10",
                8,
                BigDecimal.valueOf(1000.00),
                true
        ));
    }

    @Test
    @DisplayName("Rule 1 — Reservation start time in the past must be rejected with 400 Bad Request")
    void testPastStartTimeRejected() {
        LocalDateTime pastStart = LocalDateTime.now().minusHours(2);
        LocalDateTime futureEnd = LocalDateTime.now().plusHours(1);

        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), pastStart, futureEnd);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "user1")
        );
        assertEquals("Reservation start time cannot be in the past", ex.getMessage());
    }

    @Test
    @DisplayName("Rule 2 — End time before or equal to start time must be rejected with 400 Bad Request")
    void testEndTimeBeforeStartTimeRejected() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.minusHours(1);

        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), start, end);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "user1")
        );
        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    @DisplayName("Rule 3 — Booking unavailable resource must be rejected with 409 Conflict")
    void testUnavailableResourceRejected() {
        testRoom.setAvailable(false);
        resourceRepository.save(testRoom);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), start, end);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class, () ->
                reservationService.createReservation(request, "user1")
        );
        assertEquals("Resource is currently unavailable for booking", ex.getMessage());
    }

    @Test
    @DisplayName("Rule 4 — Overlapping reservations must be rejected with 409 Conflict")
    void testOverlappingReservationRejected() {
        // Initial booking: 10:00 - 12:00 (2 hours)
        LocalDateTime start1 = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        LocalDateTime end1 = start1.plusHours(2);
        ReservationCreateRequest request1 = new ReservationCreateRequest(testRoom.getId(), start1, end1);
        reservationService.createReservation(request1, "user1");

        // Overlapping booking: 11:00 - 13:00 (overlaps with 10:00 - 12:00)
        LocalDateTime start2 = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0);
        LocalDateTime end2 = start2.plusHours(2);
        ReservationCreateRequest request2 = new ReservationCreateRequest(testRoom.getId(), start2, end2);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class, () ->
                reservationService.createReservation(request2, "user1")
        );
        assertEquals("Resource is already booked for the selected time range", ex.getMessage());
    }

    @Test
    @DisplayName("Rule 4 — Non-overlapping reservations on the same resource must be accepted")
    void testNonOverlappingReservationAccepted() {
        LocalDateTime start1 = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0);
        LocalDateTime end1 = start1.plusHours(2);
        ReservationCreateRequest request1 = new ReservationCreateRequest(testRoom.getId(), start1, end1);
        ReservationResponse res1 = reservationService.createReservation(request1, "user1");
        assertNotNull(res1);

        // Starts after the first booking ends (12:00 - 14:00)
        LocalDateTime start2 = end1;
        LocalDateTime end2 = start2.plusHours(2);
        ReservationCreateRequest request2 = new ReservationCreateRequest(testRoom.getId(), start2, end2);
        ReservationResponse res2 = reservationService.createReservation(request2, "user1");
        assertNotNull(res2);
    }

    @Test
    @DisplayName("Rule 4 — Cancelled reservation must not block future booking in the same time range")
    void testCancelledReservationDoesNotBlock() {
        LocalDateTime start = LocalDateTime.now().plusDays(6).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), start, end);
        ReservationResponse res = reservationService.createReservation(request, "user1");

        // Cancel the reservation
        reservationService.cancelUserReservation(res.getId(), "user1");

        // Now re-book the exact same time slot -> should succeed!
        ReservationResponse newRes = reservationService.createReservation(request, "user1");
        assertNotNull(newRes);
        assertEquals(ReservationStatus.PENDING, newRes.getStatus());
    }

    @Test
    @DisplayName("Rule 5 — Reservation price must be calculated strictly server-side")
    void testServerSidePriceCalculation() {
        // Price per hour = 1000.00, booking for 3.5 hours (210 mins) -> 3500.00
        LocalDateTime start = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(3).plusMinutes(30);

        ReservationCreateRequest request = new ReservationCreateRequest(testRoom.getId(), start, end);
        ReservationResponse response = reservationService.createReservation(request, "user1");

        assertNotNull(response.getPrice());
        assertEquals(new BigDecimal("3500.00"), response.getPrice());
    }
}
