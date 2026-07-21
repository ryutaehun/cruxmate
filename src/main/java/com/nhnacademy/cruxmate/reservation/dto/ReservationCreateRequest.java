package com.nhnacademy.cruxmate.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationCreateRequest(

        @NotNull(message = "세션 ID는 필수입니다.")
        @Positive(message = "세션 ID는 양수여야 합니다.")
        Long sessionId,

        @Positive(message = "예약 인원은 1명 이상이어야 합니다.")
        int participantCount
) {}
