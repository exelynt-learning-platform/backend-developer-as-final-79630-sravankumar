package com.example.resourcebooking.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class ReservationCreateRequest {

    @NotNull(message = "Resource ID is required")
    @Positive(message = "Resource ID must be a positive integer")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    public ReservationCreateRequest() {
    }

    public ReservationCreateRequest(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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
}
