package com.nhnacademy.cruxmate.idempotency.repository;

import com.nhnacademy.cruxmate.idempotency.domain.ReservationIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationIdempotencyRepository extends JpaRepository<ReservationIdempotency, Long> {
    Optional<ReservationIdempotency> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);
}
