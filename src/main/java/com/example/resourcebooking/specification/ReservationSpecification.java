package com.example.resourcebooking.specification;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.enums.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    public static Specification<Reservation> filter(
            Long userId,
            Long resourceId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDateTime startAfter,
            LocalDateTime endBefore
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            if (resourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("resource").get("id"), resourceId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (startAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startAfter));
            }

            if (endBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endBefore));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
