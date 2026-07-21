package com.nhnacademy.cruxmate.reservation.dto;

public record ReservationCreateRequest(Long sessionId, int participantCount) {
}
