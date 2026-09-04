package com.example.resourcebooking.repository;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId " +
            "AND r.status <> :cancelledStatus " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservations(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cancelledStatus") ReservationStatus cancelledStatus
    );

    @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId " +
            "AND r.id <> :excludeReservationId " +
            "AND r.status <> :cancelledStatus " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservationsExcluding(
            @Param("resourceId") Long resourceId,
            @Param("excludeReservationId") Long excludeReservationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cancelledStatus") ReservationStatus cancelledStatus
    );
}
