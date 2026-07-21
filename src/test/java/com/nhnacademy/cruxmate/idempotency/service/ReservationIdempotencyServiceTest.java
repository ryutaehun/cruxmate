package com.nhnacademy.cruxmate.idempotency.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.idempotency.domain.IdempotencyStatus;
import com.nhnacademy.cruxmate.idempotency.domain.ReservationIdempotency;
import com.nhnacademy.cruxmate.idempotency.repository.ReservationIdempotencyRepository;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        ReservationService.class,
        ReservationIdempotencyService.class
})
class ReservationIdempotencyServiceTest {

    @Autowired
    private ReservationIdempotencyService idempotencyService;

    @Autowired
    private ReservationIdempotencyRepository idempotencyRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @Autowired
    private ReservationService reservationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 새로운_멱등성_키로_예약하면_예약과_완료_기록이_함께_저장된다() {
        Member member =
                createMember("idempotency-integration@example.com");
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

        Long reservationId = idempotencyService.createReservation(
                member.getId(),
                session.getId(),
                2,
                "integration-key-123",
                "a".repeat(64)
        );

        Long memberId = member.getId();
        Long sessionId = session.getId();

        entityManager.flush();
        entityManager.clear();

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow();

        ReservationIdempotency idempotency =
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                memberId,
                                "integration-key-123"
                        )
                        .orElseThrow();

        ClimbingSession savedSession =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow();

        assertThat(reservation.getParticipantCount()).isEqualTo(2);
        assertThat(reservation.getMember().getId()).isEqualTo(memberId);
        assertThat(reservation.getSession().getId()).isEqualTo(sessionId);

        assertThat(idempotency.getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(idempotency.getReservation().getId())
                .isEqualTo(reservationId);
        assertThat(idempotency.getCreatedAt()).isNotNull();
        assertThat(idempotency.getCompletedAt()).isNotNull();

        assertThat(savedSession.getReservedCount()).isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 예약_생성에_실패하면_멱등성_기록도_함께_롤백된다() {
        Member member =
                createMember("idempotency-rollback@example.com");

        memberRepository.saveAndFlush(member);

        Long nonexistentSessionId = 999999L;
        String idempotencyKey = "rollback-key-123";

        assertThatThrownBy(() ->
                idempotencyService.createReservation(
                        member.getId(),
                        nonexistentSessionId,
                        2,
                        idempotencyKey,
                        "a".repeat(64)
                )
        ).isInstanceOf(BusinessException.class);

        assertThat(
                idempotencyRepository.findByMemberIdAndIdempotencyKey(
                        member.getId(),
                        idempotencyKey
                )
        ).isEmpty();

        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    void 동일한_키와_동일한_요청을_다시_보내면_기존_예약_ID를_반환한다() {
        Member member =
                createMember("idempotency-retry-integration@example.com");
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

        String idempotencyKey = "retry-key-123";
        String requestHash = "a".repeat(64);

        Long firstReservationId = idempotencyService.createReservation(
                member.getId(),
                session.getId(),
                2,
                idempotencyKey,
                requestHash
        );

        Long secondReservationId = idempotencyService.createReservation(
                member.getId(),
                session.getId(),
                2,
                idempotencyKey,
                requestHash
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(secondReservationId).isEqualTo(firstReservationId);
        assertThat(reservationRepository.count()).isEqualTo(1);

        ReservationIdempotency idempotency =
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                member.getId(),
                                idempotencyKey
                        )
                        .orElseThrow();

        assertThat(idempotency.getReservation().getId())
                .isEqualTo(firstReservationId);
        assertThat(idempotency.getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
    }

    @Test
    void 동일한_키와_동일한_요청이_PROCESSING_상태이면_예약을_생성하지_않는다() {
        Member member = createMember("idempotency-processing-integration@example.com");
        memberRepository.save(member);

        String idempotencyKey = "processing-key-123";
        String requestHash = "a".repeat(64);

        ReservationIdempotency processing =
                ReservationIdempotency.create(
                        member,
                        idempotencyKey,
                        requestHash
                );

        idempotencyRepository.saveAndFlush(processing);

        assertThatThrownBy(() ->
                idempotencyService.createReservation(
                        member.getId(),
                        10L,
                        2,
                        idempotencyKey,
                        requestHash
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);

        assertThat(reservationRepository.count()).isZero();
    }
}