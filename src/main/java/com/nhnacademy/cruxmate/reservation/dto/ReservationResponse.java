package com.nhnacademy.cruxmate.reservation.dto;

import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        int participantCount,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime canceledAt,
        Long sessionId,
        String sessionTitle,
        String sessionLocation,
        LocalDateTime sessionStartAt,
        LocalDateTime sessionEndAt
) {

    public static ReservationResponse from(
            Reservation reservation
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getParticipantCount(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getCanceledAt(),
                reservation.getSession().getId(),
                reservation.getSession().getTitle(),
                reservation.getSession().getLocation(),
                reservation.getSession().getStartAt(),
                reservation.getSession().getEndAt()
        );
    }
}