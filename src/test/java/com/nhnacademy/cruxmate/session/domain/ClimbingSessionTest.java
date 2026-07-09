package com.nhnacademy.cruxmate.session.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClimbingSessionTest {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 7, 20, 19, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 7, 20, 21, 0);
    private static final LocalDateTime RESERVATION_OPEN_AT = LocalDateTime.of(2026, 7, 10, 9, 0);
    private static final LocalDateTime RESERVATION_CLOSE_AT = LocalDateTime.of(2026, 7, 20, 18, 0);

    @Test
    void 정상적인_정보로_세션을_생성한다() {
        ClimbingSession session = createSession(10);

        assertThat(session.getTitle()).isEqualTo("평일 저녁 초보 세션");
        assertThat(session.getCapacity()).isEqualTo(10);
        assertThat(session.getReservedCount()).isZero();
        assertThat(session.getStatus())
                .isEqualTo(ClimbingSessionStatus.SCHEDULED);
    }

    @Test
    void 정원이_0명이면_세션을_생성할_수_없다() {
        assertThatThrownBy(() -> createSession(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("세션 정원은 1명 이상이어야 합니다.");
    }

    @Test
    void 예약_마감이_세션_시작보다_늦으면_생성할_수_없다() {
        assertThatThrownBy(() -> createSession(
                START_AT,
                END_AT,
                RESERVATION_OPEN_AT,
                LocalDateTime.of(2026, 7, 20, 20, 0),
                10
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reserve_정상() {
        ClimbingSession session = createSession(4);

        session.reserve(3, RESERVATION_OPEN_AT);

        assertThat(session.getReservedCount()).isEqualTo(3);
    }

    @Test
    void reserve_예약시작전이면_실패() {
        ClimbingSession session = createSession(4);

        assertThatThrownBy(() -> session.reserve(
                2,
                RESERVATION_OPEN_AT.minusMinutes(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 기간이 아닙니다");
    }

    @Test
    void reserve_시작_마감_경계테스트() {
        ClimbingSession session = createSession(4);

        session.reserve(2, RESERVATION_OPEN_AT);

        assertThat(session.getReservedCount()).isEqualTo(2);
        assertThatThrownBy(() -> session.reserve(2, RESERVATION_CLOSE_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 기간이 아닙니다");
    }

    @Test
    void reserve_참여인원이_1명미만이면_실패() {
        ClimbingSession session = createSession(4);

        assertThatThrownBy(() -> session.reserve(
                0,
                RESERVATION_OPEN_AT.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void reserve_참여인원이_4명초과면_실패() {
        ClimbingSession session = createSession(5);

        assertThatThrownBy(() -> session.reserve(
                5,
                RESERVATION_OPEN_AT.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void reserve_잔여정원을_초과하면_실패() {
        ClimbingSession session = createSession(4);

        session.reserve(3, RESERVATION_OPEN_AT.plusHours(1));

        assertThatThrownBy(() -> session.reserve(
                2,
                RESERVATION_OPEN_AT.plusHours(2)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 인원이 초과되었습니다");

        assertThat(session.getReservedCount()).isEqualTo(3);
    }

    @Test
    void 예약_인원을_해제하면_reservedCount가_감소한다() {
        ClimbingSession session = createSession(10);
        session.reserve(2, RESERVATION_OPEN_AT.plusDays(1));

        session.release(1);

        assertThat(session.getReservedCount()).isEqualTo(1);
    }

    @Test
    void 취소_인원이_1명_미만이면_예외가_발생한다() {
        ClimbingSession session = createSession(10);
        session.reserve(2, RESERVATION_OPEN_AT.plusDays(1));

        assertThatThrownBy(() -> session.release(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("취소 인원은 1명 이상이어야 합니다.");

        assertThat(session.getReservedCount()).isEqualTo(2);
    }

    @Test
    void 현재_예약_인원보다_많이_해제할_수_없다() {
        ClimbingSession session = createSession(10);
        session.reserve(2, RESERVATION_OPEN_AT.plusDays(1));

        assertThatThrownBy(() -> session.release(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 인원보다 더 많은 인원을 취소할 수 없습니다.");

        assertThat(session.getReservedCount()).isEqualTo(2);
    }

    private ClimbingSession createSession(int capacity) {
        return createSession(
                START_AT,
                END_AT,
                RESERVATION_OPEN_AT,
                RESERVATION_CLOSE_AT,
                capacity
        );
    }

    private ClimbingSession createSession(
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime reservationOpenAt,
            LocalDateTime reservationCloseAt,
            int capacity
    ) {
        return ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                capacity,
                ClimbingSessionLevel.BEGINNER
        );
    }
}
