package com.example.resourcebooking.dto.reservation;

import com.example.resourcebooking.enums.ReservationStatus;

import java.time.LocalDateTime;

public class ReservationUpdateRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;

    public ReservationUpdateRequest() {
    }

    public ReservationUpdateRequest(LocalDateTime startTime, LocalDateTime endTime, ReservationStatus status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
