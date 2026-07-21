package com.nhnacademy.cruxmate.session.domain;

import org.junit.jupiter.api.Test;

import static com.nhnacademy.cruxmate.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClimbingSessionTest {

    @Test
    void 정상적인_정보로_세션을_생성한다() {
        ClimbingSession session = createSession(10);

        assertThat(session.getTitle()).isEqualTo(DEFAULT_SESSION_TITLE);
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
                SESSION_START_AT,
                SESSION_END_AT,
                RESERVATION_OPEN_AT,
                SESSION_START_AT.plusHours(1),
                10
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 예약_인원을_추가하면_reservedCount가_증가한다() {
        ClimbingSession session = createSession(4);

        session.reserve(3, RESERVATION_OPEN_AT);

        assertThat(session.getReservedCount()).isEqualTo(3);
    }

    @Test
    void 예약_시작_전이면_예약할_수_없다() {
        ClimbingSession session = createSession(4);

        assertThatThrownBy(() -> session.reserve(
                2,
                RESERVATION_OPEN_AT.minusMinutes(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 기간이 아닙니다");
    }

    @Test
    void 예약_시작_시각에는_가능하고_마감_시각에는_불가능하다() {
        ClimbingSession session = createSession(4);

        session.reserve(2, RESERVATION_OPEN_AT);

        assertThat(session.getReservedCount()).isEqualTo(2);
        assertThatThrownBy(() -> session.reserve(2, RESERVATION_CLOSE_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 기간이 아닙니다");
    }

    @Test
    void 참여인원이_1명_미만이면_예약할_수_없다() {
        ClimbingSession session = createSession(4);

        assertThatThrownBy(() -> session.reserve(
                0,
                RESERVATION_OPEN_AT.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void 참여인원이_4명_초과이면_예약할_수_없다() {
        ClimbingSession session = createSession(5);

        assertThatThrownBy(() -> session.reserve(
                5,
                RESERVATION_OPEN_AT.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void 잔여_정원을_초과하면_예약할_수_없다() {
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

}
