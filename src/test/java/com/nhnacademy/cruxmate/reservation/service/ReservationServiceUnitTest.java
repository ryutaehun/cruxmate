package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

        Assertions.assertThatThrownBy(() ->
                reservationService.createReservation(memberId, sessionId, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(climbingSessionRepository, never()).findById(anyLong());
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

        when(climbingSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() ->
                reservationService.createReservation(memberId, sessionId, 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLIMBING_SESSION_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(climbingSessionRepository).findById(sessionId);
        verify(reservationRepository, never()).save(any());
    }
}
