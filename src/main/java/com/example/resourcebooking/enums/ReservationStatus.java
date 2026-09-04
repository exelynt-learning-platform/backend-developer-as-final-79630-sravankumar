package com.example.resourcebooking.enums;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    /**
     * Validates state transitions:
     * PENDING -> CONFIRMED, CANCELLED
     * CONFIRMED -> CANCELLED
     * CANCELLED cannot transition to anything
     */
    public boolean canTransitionTo(ReservationStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
