package com.nhnacademy.cruxmate.reservation.repository;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ReservationRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 예약을_저장하면_회원과_세션의_연관관계가_함께_조회된다() {
        Member member = createMember("reservation-repository@example.com");
        memberRepository.save(member);

        ClimbingSession climbingSession = createSession(2);
        climbingSessionRepository.save(climbingSession);

        Reservation reservation = Reservation.create(
                member, climbingSession, 2
        );

        reservationRepository.save(reservation);

        Long reservationId = reservation.getId();

        entityManager.flush();

        entityManager.clear();

        Reservation res = reservationRepository.findById(reservationId).orElseThrow();
        boolean alreadyReserved = reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                member.getId(), climbingSession.getId(), ReservationStatus.CONFIRMED
        );

        assertThat(res.getId()).isEqualTo(reservationId);
        assertThat(res.getMember().getId()).isEqualTo(member.getId());
        assertThat(res.getSession().getId()).isEqualTo(climbingSession.getId());
        assertThat(res.getParticipantCount()).isEqualTo(2);
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(res.getCreatedAt()).isNotNull();
        assertThat(alreadyReserved).isTrue();
    }

    @Test
    void 확정된_예약이_존재하면_중복_예약_조회가_true를_반환한다() {
        Member member = createMember("duplicate-reservation@example.com");
        memberRepository.save(member);
        Long memberId = member.getId();

        ClimbingSession climbingSession = createSession(2);
        climbingSessionRepository.save(climbingSession);
        Long sessionId = climbingSession.getId();

        Reservation reservation = Reservation.create(
                member, climbingSession, 2
        );

        reservationRepository.save(reservation);

        entityManager.flush();

        entityManager.clear();

        boolean alreadyReserved =
                reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                        memberId,
                        sessionId,
                        ReservationStatus.CONFIRMED
                );

        assertThat(alreadyReserved).isTrue();

        boolean canceledReservationExists =
                reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                        memberId,
                        sessionId,
                        ReservationStatus.CANCELED
                );

        assertThat(canceledReservationExists).isFalse();
    }
}
