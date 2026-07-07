package com.nhnacademy.cruxmate.session.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClimbingSessionTest {

    @Test
    void 정상적인_정보로_세션을_생성한다() {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        ClimbingSession session = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                10,
                ClimbingSessionLevel.BEGINNER
        );

        assertThat(session.getTitle()).isEqualTo("평일 저녁 초보 세션");
        assertThat(session.getCapacity()).isEqualTo(10);
        assertThat(session.getReservedCount()).isZero();
        assertThat(session.getStatus())
                .isEqualTo(ClimbingSessionStatus.SCHEDULED);
    }

    @Test
    void 정원이_0명이면_세션을_생성할_수_없다() {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        assertThatThrownBy(() -> ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                0,
                ClimbingSessionLevel.BEGINNER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("세션 정원은 1명 이상이어야 합니다.");
    }

    @Test
    void 예약_마감이_세션_시작보다_늦으면_생성할_수_없다() {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 20, 0);

        assertThatThrownBy(() -> ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                10,
                ClimbingSessionLevel.BEGINNER
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }
}