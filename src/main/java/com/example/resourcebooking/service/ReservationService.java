package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.common.PageResponse;
import com.example.resourcebooking.dto.reservation.ReservationCreateRequest;
import com.example.resourcebooking.dto.reservation.ReservationResponse;
import com.example.resourcebooking.dto.reservation.ReservationUpdateRequest;
import com.example.resourcebooking.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ReservationService {

    ReservationResponse createReservation(ReservationCreateRequest request, String currentUsername);

    PageResponse<ReservationResponse> getUserReservations(
            String currentUsername,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ReservationResponse getUserReservationById(Long reservationId, String currentUsername);

    ReservationResponse updateUserReservation(Long reservationId, ReservationUpdateRequest request, String currentUsername);

    void cancelUserReservation(Long reservationId, String currentUsername);

    PageResponse<ReservationResponse> getAllReservations(
            Long userId,
            Long resourceId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDateTime startAfter,
            LocalDateTime endBefore,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ReservationResponse getReservationById(Long reservationId);

    ReservationResponse updateReservationByAdmin(Long reservationId, ReservationUpdateRequest request);

    void deleteReservationByAdmin(Long reservationId);
}
