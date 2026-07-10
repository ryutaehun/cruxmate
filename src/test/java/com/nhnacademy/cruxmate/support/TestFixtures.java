package com.nhnacademy.cruxmate.support;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;

import java.time.LocalDateTime;

public final class TestFixtures {

    public static final String DEFAULT_PASSWORD_HASH = "password-hash";
    public static final String DEFAULT_SESSION_TITLE = "평일 저녁 초보 세션";
    public static final String DEFAULT_SESSION_LOCATION = "광주 온클라이밍";

    public static final LocalDateTime SESSION_START_AT =
            LocalDateTime.of(2026, 7, 20, 19, 0);
    public static final LocalDateTime SESSION_END_AT =
            LocalDateTime.of(2026, 7, 20, 21, 0);
    public static final LocalDateTime RESERVATION_OPEN_AT =
            LocalDateTime.of(2026, 7, 10, 9, 0);
    public static final LocalDateTime RESERVATION_CLOSE_AT =
            LocalDateTime.of(2026, 7, 20, 18, 0);

    private TestFixtures() {
    }

    public static Member createMember() {
        return createMember("member@example.com");
    }

    public static Member createMember(String email) {
        return Member.create(email, DEFAULT_PASSWORD_HASH);
    }

    public static ClimbingSession createSession() {
        return createSession(4);
    }

    public static ClimbingSession createSession(int capacity) {
        return createSession(
                SESSION_START_AT,
                SESSION_END_AT,
                RESERVATION_OPEN_AT,
                RESERVATION_CLOSE_AT,
                capacity
        );
    }

    public static ClimbingSession createSession(
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime reservationOpenAt,
            LocalDateTime reservationCloseAt,
            int capacity
    ) {
        return ClimbingSession.create(
                DEFAULT_SESSION_TITLE,
                DEFAULT_SESSION_LOCATION,
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                capacity,
                ClimbingSessionLevel.BEGINNER
        );
    }

    public static Reservation createReservation(Member member) {
        return Reservation.create(
                member,
                createSession(),
                2
        );
    }
}
