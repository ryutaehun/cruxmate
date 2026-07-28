package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.config.TimeConfig;
import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.dto.ReservationResponse;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, TimeConfig.class, ReservationService.class})
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 예약을_생성하면_예약이_저장되고_세션_예약인원이_증가한다() {
        Member member = createMember("reservation-create@example.com");
        memberRepository.save(member);

        LocalDateTime now = LocalDateTime.now();
        ClimbingSession session = createSession(
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.minusDays(1),
                now.plusDays(1),
                5
        );
        climbingSessionRepository.save(session);

        Long reservationId = reservationService.createReservation(member.getId(), session.getId(), 2);

        entityManager.flush();
        entityManager.clear();

        Reservation savedReservation = reservationRepository.findById(reservationId).orElseThrow(
                () -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
        );
        ClimbingSession savedClimbingSession = climbingSessionRepository.findById(session.getId()).orElseThrow(
                () -> new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND)
        );

        assertThat(reservationId).isNotNull();

        assertThat(savedReservation.getParticipantCount()).isEqualTo(2);
        assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(savedReservation.getMember().getId()).isEqualTo(member.getId());
        assertThat(savedReservation.getSession().getId()).isEqualTo(session.getId());
        assertThat(savedClimbingSession.getReservedCount()).isEqualTo(2);
    }

    @Test
    void 예약을_취소하면_상태가_변경되고_세션_예약인원이_감소한다() {
        Member member = createMember("reservation-cancel@example.com");
        memberRepository.save(member);

        LocalDateTime now = LocalDateTime.now();

        ClimbingSession session = createSession(
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.minusDays(1),
                now.plusDays(1),
                5
        );
        climbingSessionRepository.save(session);

        Long reservationId = reservationService.createReservation(
                member.getId(),
                session.getId(),
                2
        );

        entityManager.flush();
        entityManager.clear();

        Long canceledReservationId = reservationService.cancelReservation(member.getId(), reservationId);

        entityManager.flush();
        entityManager.clear();

        Reservation canceledReservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow();

        ClimbingSession updatedSession =
                climbingSessionRepository.findById(session.getId())
                        .orElseThrow();

        assertThat(canceledReservationId).isEqualTo(reservationId);
        assertThat(canceledReservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELED);
        assertThat(canceledReservation.getCanceledAt()).isNotNull();
        assertThat(updatedSession.getReservedCount()).isZero();

    }

    @Test
    void 회원의_예약만_조회하고_세션_정보를_응답에_포함한다() {
        Member memberA = createMember("member-a@example.com");
        Member memberB = createMember("member-b@example.com");
        memberRepository.saveAllAndFlush(List.of(memberA, memberB));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        ClimbingSession sessionA1 = createSession(
                "A의 첫 번째 세션",
                now.plusDays(1),
                now.plusDays(1).plusHours(2)
        );

        ClimbingSession sessionA2 = createSession(
                "A의 두 번째 세션",
                now.plusDays(2),
                now.plusDays(2).plusHours(2)
        );

        ClimbingSession sessionB = createSession(
                "B의 세션",
                now.plusDays(3),
                now.plusDays(3).plusHours(2)
        );

        climbingSessionRepository.saveAllAndFlush(
                List.of(sessionA1, sessionA2, sessionB)
        );

        Reservation memberAReservation1 =
                Reservation.create(memberA, sessionA1, 1);

        Reservation memberAReservation2 =
                Reservation.create(memberA, sessionA2, 2);

        Reservation memberBReservation =
                Reservation.create(memberB, sessionB, 3);

        reservationRepository.saveAllAndFlush(
                List.of(
                        memberAReservation1,
                        memberAReservation2,
                        memberBReservation
                )
        );

        entityManager.clear();

        PageResponse<ReservationResponse> response =
                reservationService.getMyReservations(
                        memberA.getId(),
                        0,
                        10
                );

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);

        assertThat(response.content())
                .extracting(ReservationResponse::reservationId)
                .containsExactlyInAnyOrder(
                        memberAReservation1.getId(),
                        memberAReservation2.getId()
                );

        assertThat(response.content())
                .extracting(ReservationResponse::sessionTitle)
                .containsExactlyInAnyOrder(
                        "A의 첫 번째 세션",
                        "A의 두 번째 세션"
                );

        assertThat(response.content())
                .extracting(ReservationResponse::sessionTitle)
                .doesNotContain("B의 세션");

        ReservationResponse sessionA2Response =
                response.content().stream()
                        .filter(reservation ->
                                reservation.reservationId()
                                        .equals(memberAReservation2.getId())
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(sessionA2Response.participantCount()).isEqualTo(2);
        assertThat(sessionA2Response.status())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(sessionA2Response.sessionId())
                .isEqualTo(sessionA2.getId());
        assertThat(sessionA2Response.sessionLocation())
                .isEqualTo("광주 온클라이밍");
        assertThat(sessionA2Response.sessionStartAt())
                .isEqualTo(sessionA2.getStartAt());
    }

}
