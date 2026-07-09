package com.nhnacademy.cruxmate.reservation.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReservationTest {

    @Test
    void 정상_생성() {
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
    void participantCount가_0이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                createMember(), createSession(), 0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void participantCount가_5이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                createMember(), createSession(), 5
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void member가_null이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                null, createSession(), 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 회원은 필수입니다.");
    }

    @Test
    void session이_null이면_실패() {
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

    private Member createMember() {
        return Member.create("1234@gmail.com", "1234");
    }

    private ClimbingSession createSession() {
        return ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                LocalDateTime.of(2026, 7, 20, 19, 0),
                LocalDateTime.of(2026, 7, 20, 21, 0),
                LocalDateTime.of(2026, 7, 10, 9, 0),
                LocalDateTime.of(2026, 7, 20, 18, 0),
                4,
                ClimbingSessionLevel.BEGINNER
        );
    }
}
