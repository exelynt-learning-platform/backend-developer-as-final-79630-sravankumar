package com.example.resourcebooking.mapper;

import com.example.resourcebooking.dto.reservation.ReservationResponse;
import com.example.resourcebooking.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource() != null ? reservation.getResource().getId() : null,
                reservation.getResource() != null ? reservation.getResource().getName() : null,
                reservation.getUser() != null ? reservation.getUser().getId() : null,
                reservation.getUser() != null ? reservation.getUser().getUsername() : null,
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
