package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ClimbingSessionRepository climbingSessionRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void 회원이_없으면_MEMBER_NOT_FOUND(){
        Long memberId = 1L;
        Long sessionId = 1L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.createReservation(memberId, sessionId, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(climbingSessionRepository, never()).findByIdForUpdate(anyLong());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 세션이_없으면_CLIMBING_SESSION_NOT_FOUND(){
        Long memberId = 1L;
        Long sessionId = 1L;

        Member member = Member.create(
                "fbxogns321@naver.com",
                "1234"
        );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(climbingSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.createReservation(memberId, sessionId, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLIMBING_SESSION_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(climbingSessionRepository).findByIdForUpdate(sessionId);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 이미_확정된_예약이_있으면_DUPLICATE_RESERVATION(){
        Long memberId = 1L;
        Long sessionId = 1L;

        Member member = Member.create(
                "fbxogns321@naver.com",
                "1234"
        );

        LocalDateTime startAt = LocalDateTime.of(2026, 7, 20, 19, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 20, 21, 0);
        LocalDateTime reservationOpenAt =
                LocalDateTime.of(2026, 7, 10, 9, 0);
        LocalDateTime reservationCloseAt =
                LocalDateTime.of(2026, 7, 20, 18, 0);

        ClimbingSession session = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                4,
                ClimbingSessionLevel.BEGINNER
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(climbingSessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                memberId, sessionId, ReservationStatus.CONFIRMED
        )).thenReturn(true);

        assertThatThrownBy(
                () -> reservationService.createReservation(memberId, sessionId, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESERVATION);

        verify(reservationRepository, never()).save(any());
        verify(reservationRepository)
                .existsByMember_IdAndSession_IdAndStatus(
                        memberId,
                        sessionId,
                        ReservationStatus.CONFIRMED
                );
        assertThat(session.getReservedCount()).isZero();
    }
}
