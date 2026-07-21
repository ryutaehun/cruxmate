package com.nhnacademy.cruxmate.session.dto;

import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionStatus;

import java.time.LocalDateTime;

public record ClimbingSessionResponse (Long sessionId, String title, String location,
                                       LocalDateTime startAt, LocalDateTime endAt, LocalDateTime reservationOpenAt,
                                       LocalDateTime reservationCloseAt, int capacity, int reservedCount, int remainingCapacity,
                                       ClimbingSessionLevel level, ClimbingSessionStatus status){
    public static ClimbingSessionResponse from(ClimbingSession session) {
        return new ClimbingSessionResponse(session.getId(), session.getTitle(), session.getLocation(), session.getStartAt(),
                session.getEndAt(), session.getReservationOpenAt(), session.getReservationCloseAt(), session.getCapacity(),
                session.getReservedCount(), session.getCapacity() - session.getReservedCount(), session.getLevel(), session.getStatus());
    }
}
