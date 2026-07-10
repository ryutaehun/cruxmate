package com.nhnacademy.cruxmate.idempotency.domain;

import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createReservation;

class ReservationIdempotencyTest {

    @Test
    void 멱등성_요청을_PROCESSING_상태로_생성한다() {
        Member member = createMember();
        String idempotencyKey = "reservation-key-123";
        String requestHash = "a".repeat(64);

        ReservationIdempotency idempotency =
                ReservationIdempotency.create(
                        member,
                        idempotencyKey,
                        requestHash
                );

        assertThat(idempotency.getMember()).isSameAs(member);
        assertThat(idempotency.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(idempotency.getRequestHash()).isEqualTo(requestHash);
        assertThat(idempotency.getStatus())
                .isEqualTo(IdempotencyStatus.PROCESSING);
        assertThat(idempotency.getReservation()).isNull();
        assertThat(idempotency.getCompletedAt()).isNull();
    }

    @Test
    void 예약_처리를_완료하면_COMPLETED_상태가_된다() {
        Member member = createMember();
        Reservation reservation = createReservation(member);

        ReservationIdempotency idempotency =
                ReservationIdempotency.create(
                        member,
                        "reservation-key-123",
                        "a".repeat(64)
                );

        LocalDateTime completedAt =
                LocalDateTime.of(2026, 7, 10, 10, 30);

        idempotency.complete(reservation, completedAt);

        assertThat(idempotency.getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(idempotency.getReservation()).isSameAs(reservation);
        assertThat(idempotency.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void 이미_완료된_멱등성_요청은_다시_완료할_수_없다() {
        Member member = createMember();
        Reservation reservation = createReservation(member);

        ReservationIdempotency idempotency =
                ReservationIdempotency.create(
                        member,
                        "reservation-key-123",
                        "a".repeat(64)
                );

        idempotency.complete(
                reservation,
                LocalDateTime.of(2026, 7, 10, 10, 30)
        );

        assertThatThrownBy(() ->
                idempotency.complete(
                        reservation,
                        LocalDateTime.of(2026, 7, 10, 10, 31)
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 멱등성 요청입니다.");
    }

}
