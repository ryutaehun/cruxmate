package com.nhnacademy.cruxmate.session.dto;

import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ClimbingSessionCreateRequest(

        @NotBlank(message = "세션 제목은 필수입니다.")
        String title,

        @NotBlank(message = "세션 장소는 필수입니다.")
        String location,

        @NotNull(message = "세션 시작 시간은 필수입니다.")
        LocalDateTime startAt,

        @NotNull(message = "세션 종료 시간은 필수입니다.")
        LocalDateTime endAt,

        @NotNull(message = "예약 시작 시간은 필수입니다.")
        LocalDateTime reservationOpenAt,

        @NotNull(message = "예약 종료 시간은 필수입니다.")
        LocalDateTime reservationCloseAt,

        @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
        int capacity,

        @NotNull(message = "세션 난이도는 필수입니다.")
        ClimbingSessionLevel level
) {
}