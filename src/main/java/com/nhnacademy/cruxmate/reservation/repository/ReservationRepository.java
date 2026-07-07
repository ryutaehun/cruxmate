package com.nhnacademy.cruxmate.reservation.repository;

import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Long, Reservation> {
}
