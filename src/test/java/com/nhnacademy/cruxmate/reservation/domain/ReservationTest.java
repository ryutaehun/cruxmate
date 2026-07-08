package com.nhnacademy.cruxmate.reservation.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReservationTest {

    private Member member;
    private ClimbingSession session;

    @BeforeEach
    void setUp() {
        member = Member.create("1234@gmail.com", "1234");

        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        session = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                10,
                ClimbingSessionLevel.BEGINNER
        );

    }

    @Test
    void 정상_생성() {
        Reservation reservation = Reservation.create(
                member, session, 2
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, reservation.getParticipantCount()),
                () -> Assertions.assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus()),
                () -> assertThat(reservation.getMember()).isSameAs(member),
                () -> assertThat(reservation.getSession()).isSameAs(session)
        );
    }

    @Test
    void participantCount가_0이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                member, session, 0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void participantCount가_5이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                member, session, 5
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참여 인원은 1명 이상 4명 이하여야 합니다.");
    }

    @Test
    void member가_null이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                null, session, 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 회원은 필수입니다.");
    }

    @Test
    void session이_null이면_실패() {
        assertThatThrownBy(() -> Reservation.create(
                member, null, 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 세션은 필수입니다.");
    }
}
