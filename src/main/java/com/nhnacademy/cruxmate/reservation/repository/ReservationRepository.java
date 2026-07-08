package com.nhnacademy.cruxmate.reservation.repository;

import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByMember_IdAndSession_IdAndStatus(Long memberId, Long sessionId, ReservationStatus status);
}
