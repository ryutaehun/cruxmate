package com.nhnacademy.cruxmate.reservation.repository;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
public class ReservationRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 예약을_저장하면_회원과_세션의_연관관계가_함께_조회된다(){
        Member member = Member.create("abc@naver.com", "1234");
        memberRepository.save(member);
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        ClimbingSession climbingSession = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                2,
                ClimbingSessionLevel.BEGINNER
        );
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

        Assertions.assertAll(
                () -> Assertions.assertEquals(reservationId, res.getId()),
                () -> Assertions.assertEquals(member.getId(), res.getMember().getId()),
                () -> Assertions.assertEquals(climbingSession.getId(), res.getSession().getId()),
                () -> Assertions.assertEquals(2, res.getParticipantCount()),
                () -> Assertions.assertEquals(ReservationStatus.CONFIRMED, res.getStatus()),
                () -> Assertions.assertNotNull(res.getCreatedAt()),
                () -> Assertions.assertTrue(alreadyReserved)
        );
    }

    @Test
    void 확정된_예약이_존재하면_중복_예약_조회가_true를_반환한다(){
        Member member = Member.create("abc@naver.com", "1234");
        memberRepository.save(member);
        Long memberId = member.getId();

        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        ClimbingSession climbingSession = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                2,
                ClimbingSessionLevel.BEGINNER
        );
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

        Assertions.assertTrue(alreadyReserved);

        boolean canceledReservationExists =
                reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                        memberId,
                        sessionId,
                        ReservationStatus.CANCELED
                );

        Assertions.assertFalse(canceledReservationExists);
    }
}
