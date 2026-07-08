package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ReservationService.class})
public class ReservationServiceTest {

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
        Member member = Member.create("fbxogns321@naver.com", "1234");
        memberRepository.save(member);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationOpenAt = now.minusDays(1);
        LocalDateTime reservationCloseAt = now.plusDays(1);
        LocalDateTime startAt = now.plusDays(2);
        LocalDateTime endAt = now.plusDays(2).plusHours(2);

        ClimbingSession session = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                5,
                ClimbingSessionLevel.BEGINNER
        );
        climbingSessionRepository.save(session);

        Long reservationId = reservationService.createReservation(member.getId(), session.getId(), 2);

        entityManager.flush();
        entityManager.clear();

        Reservation savedReservation = reservationRepository.findById(reservationId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );
        ClimbingSession savedClimbingSession = climbingSessionRepository.findById(session.getId()).orElseThrow(
                () -> new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND)
        );

        assertThat(reservationId).isNotNull();

        Assertions.assertAll(
                () -> assertThat(savedReservation.getParticipantCount()).isEqualTo(2),
                () -> assertThat(savedReservation.getStatus())
                        .isEqualTo(ReservationStatus.CONFIRMED),
                () -> assertThat(savedReservation.getMember().getId())
                        .isEqualTo(member.getId()),
                () -> assertThat(savedReservation.getSession().getId())
                        .isEqualTo(session.getId()),
                () -> assertThat(savedClimbingSession.getReservedCount()).isEqualTo(2)
        );
    }
}
