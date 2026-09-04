package com.example.resourcebooking.service.impl;

import com.example.resourcebooking.dto.common.PageResponse;
import com.example.resourcebooking.dto.reservation.ReservationCreateRequest;
import com.example.resourcebooking.dto.reservation.ReservationResponse;
import com.example.resourcebooking.dto.reservation.ReservationUpdateRequest;
import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.ReservationStatus;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationConflictException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.exception.UnauthorizedException;
import com.example.resourcebooking.mapper.ReservationMapper;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;
import com.example.resourcebooking.service.ReservationService;
import com.example.resourcebooking.specification.ReservationSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "price", "status", "createdAt"
    );

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, String currentUsername) {
        log.info("Processing reservation request for user '{}' on resource ID: {}", currentUsername, request.getResourceId());

        validateTimeRange(request.getStartTime(), request.getEndTime());

        // Pessimistic lock resource row to serialize concurrent booking attempts
        Resource resource = resourceRepository.findByIdWithLock(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (!resource.isAvailable()) {
            log.warn("Reservation rejected: Resource '{}' is not available", resource.getName());
            throw new ReservationConflictException("Resource is currently unavailable for booking");
        }

        // Check for overlapping active reservations
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                ReservationStatus.CANCELLED
        );

        if (!overlaps.isEmpty()) {
            log.warn("Reservation conflict: Resource '{}' is already booked for the selected time range", resource.getName());
            throw new ReservationConflictException("Resource is already booked for the selected time range");
        }

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        BigDecimal calculatedPrice = calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime());

        Reservation reservation = new Reservation(
                resource,
                user,
                request.getStartTime(),
                request.getEndTime(),
                calculatedPrice,
                ReservationStatus.PENDING
        );

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation successfully created with ID: {} for user: {}", savedReservation.getId(), currentUsername);

        return reservationMapper.toResponse(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getUserReservations(
            String currentUsername,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        Pageable pageable = createValidatedPageable(page, size, sortBy, sortDir);
        Specification<Reservation> spec = ReservationSpecification.filter(
                user.getId(), null, status, minPrice, maxPrice, null, null
        );

        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);
        return PageResponse.of(reservationPage.map(reservationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getUserReservationById(Long reservationId, String currentUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        validateOwnership(reservation, currentUsername);
        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse updateUserReservation(Long reservationId, ReservationUpdateRequest request, String currentUsername) {
        log.info("User '{}' attempting to update reservation ID: {}", currentUsername, reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        validateOwnership(reservation, currentUsername);

        if (request.getStatus() != null) {
            validateStatusTransition(reservation.getStatus(), request.getStatus());
            reservation.setStatus(request.getStatus());
        }

        if (request.getStartTime() != null || request.getEndTime() != null) {
            LocalDateTime newStart = request.getStartTime() != null ? request.getStartTime() : reservation.getStartTime();
            LocalDateTime newEnd = request.getEndTime() != null ? request.getEndTime() : reservation.getEndTime();

            validateTimeRange(newStart, newEnd);

            Resource resource = resourceRepository.findByIdWithLock(reservation.getResource().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Associated resource not found"));

            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsExcluding(
                    resource.getId(),
                    reservation.getId(),
                    newStart,
                    newEnd,
                    ReservationStatus.CANCELLED
            );

            if (!overlaps.isEmpty()) {
                throw new ReservationConflictException("Resource is already booked for the selected time range");
            }

            reservation.setStartTime(newStart);
            reservation.setEndTime(newEnd);
            reservation.setPrice(calculatePrice(resource.getPricePerHour(), newStart, newEnd));
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation ID: {} successfully updated by user '{}'", reservationId, currentUsername);
        return reservationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelUserReservation(Long reservationId, String currentUsername) {
        log.info("User '{}' cancelling reservation ID: {}", currentUsername, reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        validateOwnership(reservation, currentUsername);

        validateStatusTransition(reservation.getStatus(), ReservationStatus.CANCELLED);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation ID: {} successfully cancelled by user '{}'", reservationId, currentUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getAllReservations(
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
    ) {
        Pageable pageable = createValidatedPageable(page, size, sortBy, sortDir);
        Specification<Reservation> spec = ReservationSpecification.filter(
                userId, resourceId, status, minPrice, maxPrice, startAfter, endBefore
        );

        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);
        return PageResponse.of(reservationPage.map(reservationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationByAdmin(Long reservationId, ReservationUpdateRequest request) {
        log.info("Admin updating reservation ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (request.getStatus() != null) {
            validateStatusTransition(reservation.getStatus(), request.getStatus());
            reservation.setStatus(request.getStatus());
        }

        if (request.getStartTime() != null || request.getEndTime() != null) {
            LocalDateTime newStart = request.getStartTime() != null ? request.getStartTime() : reservation.getStartTime();
            LocalDateTime newEnd = request.getEndTime() != null ? request.getEndTime() : reservation.getEndTime();

            validateTimeRange(newStart, newEnd);

            Resource resource = resourceRepository.findByIdWithLock(reservation.getResource().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Associated resource not found"));

            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsExcluding(
                    resource.getId(),
                    reservation.getId(),
                    newStart,
                    newEnd,
                    ReservationStatus.CANCELLED
            );

            if (!overlaps.isEmpty()) {
                throw new ReservationConflictException("Resource is already booked for the selected time range");
            }

            reservation.setStartTime(newStart);
            reservation.setEndTime(newEnd);
            reservation.setPrice(calculatePrice(resource.getPricePerHour(), newStart, newEnd));
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation ID: {} successfully updated by Admin", reservationId);
        return reservationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReservationByAdmin(Long reservationId) {
        log.info("Admin deleting reservation ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        reservationRepository.delete(reservation);
        log.info("Reservation ID: {} successfully deleted by Admin", reservationId);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time must both be provided");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private void validateOwnership(Reservation reservation, String currentUsername) {
        if (!reservation.getUser().getUsername().equals(currentUsername)) {
            log.warn("Security violation: User '{}' tried to access reservation ID: {} owned by '{}'",
                    currentUsername, reservation.getId(), reservation.getUser().getUsername());
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }
    }

    private void validateStatusTransition(ReservationStatus current, ReservationStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new BadRequestException("Invalid reservation status transition from " + current + " to " + target);
        }
    }

    private BigDecimal calculatePrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private Pageable createValidatedPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be less than zero");
        }
        if (size <= 0) {
            throw new BadRequestException("Page size must be greater than zero");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed maximum limit of " + MAX_PAGE_SIZE);
        }

        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "id";
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BadRequestException("Invalid sort field: '" + field + "'. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
