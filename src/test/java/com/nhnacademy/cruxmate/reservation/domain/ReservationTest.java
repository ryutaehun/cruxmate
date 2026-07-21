package com.nhnacademy.cruxmate.reservation.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void 예약을_생성하면_CONFIRMED_상태가_된다() {
        Member member = createMember();
        ClimbingSession session = createSession();

        Reservation reservation = Reservation.create(
                member, session, 2
        );

        assertThat(reservation.getParticipantCount()).isEqualTo(2);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getMember()).isSameAs(member);
        assertThat(reservation.getSession()).isSameAs(session);
    }

    @Test
    void 참여인원이_0명이면_예약을_생성할_수_없다() {
        assertThatThrownBy(() -> Reservation.create(
                createMember(), createSession(), 0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void 참여인원이_5명이면_예약을_생성할_수_없다() {
        assertThatThrownBy(() -> Reservation.create(
                createMember(), createSession(), 5
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void 회원이_null이면_예약을_생성할_수_없다() {
        assertThatThrownBy(() -> Reservation.create(
                null, createSession(), 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 회원은 필수입니다.");
    }

    @Test
    void 세션이_null이면_예약을_생성할_수_없다() {
        assertThatThrownBy(() -> Reservation.create(
                createMember(), null, 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 세션은 필수입니다.");
    }

    @Test
    void 예약을_취소하면_상태와_취소시간이_변경된다() {
        Reservation reservation = Reservation.create(
                createMember(), createSession(), 2
        );

        LocalDateTime now = LocalDateTime.now();

        reservation.cancel(now);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getCanceledAt()).isEqualTo(now);
    }

    @Test
    void 이미_취소된_예약은_다시_취소할_수_없다() {
        Reservation reservation = Reservation.create(
                createMember(), createSession(), 2
        );

        reservation.cancel(LocalDateTime.now());

        assertThatThrownBy(() -> reservation.cancel(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

}
