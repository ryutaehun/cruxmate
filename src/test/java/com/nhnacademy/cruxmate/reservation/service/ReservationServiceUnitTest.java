package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ClimbingSessionRepository climbingSessionRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-07-15T10:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void 회원이_없으면_MEMBER_NOT_FOUND를_던진다() {
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
    void 세션이_없으면_CLIMBING_SESSION_NOT_FOUND를_던진다() {
        Long memberId = 1L;
        Long sessionId = 1L;

        Member member = createMember();

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
    void 이미_확정된_예약이_있으면_DUPLICATE_RESERVATION을_던진다() {
        Long memberId = 1L;
        Long sessionId = 1L;

        Member member = createMember();
        ClimbingSession session = createSession();

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

    @Test
    void 예약을_정상적으로_취소한다() {
        Long memberId = 1L;
        Long reservationId = 10L;
        Long sessionId = 100L;
        int participantCount = 2;

        ClimbingSession session = mock(ClimbingSession.class);
        Reservation reservation = mock(Reservation.class);

        when(reservationRepository.findSessionIdByIdAndMemberId(reservationId, memberId))
                .thenReturn(Optional.of(sessionId));
        when(climbingSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));
        when(reservationRepository.findByIdAndMemberIdForUpdate(reservationId, memberId))
                .thenReturn(Optional.of(reservation));
        when(reservation.getParticipantCount())
                .thenReturn(participantCount);
        Long result = reservationService.cancelReservation(memberId, reservationId);

        assertThat(result).isEqualTo(reservationId);

        InOrder inOrder = inOrder(reservationRepository, climbingSessionRepository, reservation, session);

        inOrder.verify(reservationRepository).findSessionIdByIdAndMemberId(reservationId, memberId);
        inOrder.verify(climbingSessionRepository).findByIdForUpdate(sessionId);
        inOrder.verify(reservationRepository).findByIdAndMemberIdForUpdate(reservationId, memberId);
        inOrder.verify(reservation).cancel(any(LocalDateTime.class));
        inOrder.verify(reservation).getParticipantCount();
        inOrder.verify(session).release(participantCount);
    }

    @Test
    void 예약을_찾을_수_없으면_취소에_실패한다() {
        Long memberId = 1L;
        Long reservationId = 10L;

        when(reservationRepository.findSessionIdByIdAndMemberId(reservationId, memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.cancelReservation(memberId, reservationId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);

        verify(climbingSessionRepository, never()).findByIdForUpdate(anyLong());
        verify(reservationRepository, never()).findByIdAndMemberIdForUpdate(anyLong(), anyLong());
    }

    @Test
    void 예약의_세션을_찾을_수_없으면_취소에_실패한다() {
        Long memberId = 1L;
        Long reservationId = 10L;
        Long sessionId = 100L;

        when(reservationRepository.findSessionIdByIdAndMemberId(reservationId, memberId))
                .thenReturn(Optional.of(sessionId));
        when(climbingSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.cancelReservation(memberId, reservationId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLIMBING_SESSION_NOT_FOUND);

        verify(reservationRepository, never()).findByIdAndMemberIdForUpdate(anyLong(), anyLong());

    }

    @Test
    void 예약을_락으로_조회하지_못하면_세션_인원이_감소하지_않는다() {
        Long memberId = 1L;
        Long reservationId = 10L;
        Long sessionId = 100L;

        ClimbingSession session = mock(ClimbingSession.class);

        when(reservationRepository.findSessionIdByIdAndMemberId(reservationId, memberId))
                .thenReturn(Optional.of(sessionId));
        when(climbingSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));
        when(reservationRepository.findByIdAndMemberIdForUpdate(reservationId, memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.cancelReservation(memberId, reservationId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND)
                );

        verify(session, never()).release(anyInt());
    }
}
