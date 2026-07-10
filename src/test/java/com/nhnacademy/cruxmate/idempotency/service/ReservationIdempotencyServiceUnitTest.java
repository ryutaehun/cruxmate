package com.nhnacademy.cruxmate.idempotency.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.idempotency.domain.IdempotencyStatus;
import com.nhnacademy.cruxmate.idempotency.domain.ReservationIdempotency;
import com.nhnacademy.cruxmate.idempotency.repository.ReservationIdempotencyRepository;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationIdempotencyServiceUnitTest {

    @Mock
    private ReservationIdempotencyRepository idempotencyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationIdempotencyService reservationIdempotencyService;

    @Test
    void 새로운_멱등성_키로_예약을_생성하고_완료_상태로_변경한다(){
        Long memberId = 1L;
        Long sessionId = 10L;
        Long reservationId = 100L;

        Member member = createMember("idempotency-service@example.com");
        Reservation reservation = mock(Reservation.class);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(reservationService.createReservation(memberId, sessionId, 2))
                .thenReturn(reservationId);

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        Long result = reservationIdempotencyService.createReservation(
                memberId, sessionId, 2, "reservation-key-123", "a".repeat(64)
        );


        ArgumentCaptor<ReservationIdempotency> captor =
                ArgumentCaptor.forClass(ReservationIdempotency.class);

        verify(idempotencyRepository)
                .saveAndFlush(captor.capture());

        ReservationIdempotency savedIdempotency =
                captor.getValue();

        assertThat(result).isEqualTo(reservationId);
        assertThat(savedIdempotency.getMember()).isSameAs(member);
        assertThat(savedIdempotency.getIdempotencyKey())
                .isEqualTo("reservation-key-123");
        assertThat(savedIdempotency.getRequestHash())
                .isEqualTo("a".repeat(64));
        assertThat(savedIdempotency.getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(savedIdempotency.getReservation())
                .isSameAs(reservation);
        assertThat(savedIdempotency.getCompletedAt())
                .isNotNull();

        InOrder inOrder = inOrder(
                idempotencyRepository,
                reservationService
        );

        inOrder.verify(idempotencyRepository)
                .saveAndFlush(any(ReservationIdempotency.class));

        inOrder.verify(reservationService)
                .createReservation(memberId, sessionId, 2);
    }

    @Test
    void 회원이_존재하지_않으면_멱등성_기록과_예약을_생성하지_않는다() {
        Long memberId = 999L;
        String idempotencyKey = "reservation-key-123";

        when(idempotencyRepository.findByMemberIdAndIdempotencyKey(
                memberId,
                idempotencyKey
        )).thenReturn(Optional.empty());

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationIdempotencyService.createReservation(
                        memberId,
                        10L,
                        2,
                        idempotencyKey,
                        "a".repeat(64)
                )
        ).isInstanceOf(BusinessException.class);

        verify(idempotencyRepository)
                .findByMemberIdAndIdempotencyKey(
                        memberId,
                        idempotencyKey
                );

        verify(memberRepository).findById(memberId);

        verify(idempotencyRepository, never())
                .saveAndFlush(any(ReservationIdempotency.class));

        verifyNoInteractions(
                reservationRepository,
                reservationService
        );
    }

    @Test
    void 동일한_키와_동일한_요청이_완료되어_있으면_기존_예약_ID를_반환한다() {
        Long memberId = 1L;
        Long reservationId = 100L;
        String idempotencyKey = "reservation-key-123";
        String requestHash = "a".repeat(64);

        Member member = createMember(
                "idempotency-retry@example.com"
        );

        Reservation reservation = mock(Reservation.class);
        when(reservation.getId()).thenReturn(reservationId);

        ReservationIdempotency existing =
                ReservationIdempotency.create(
                        member,
                        idempotencyKey,
                        requestHash
                );

        existing.complete(
                reservation,
                LocalDateTime.now()
        );

        when(idempotencyRepository.findByMemberIdAndIdempotencyKey(
                memberId,
                idempotencyKey
        )).thenReturn(Optional.of(existing));

        Long result = reservationIdempotencyService.createReservation(
                memberId,
                10L,
                2,
                idempotencyKey,
                requestHash
        );

        Assertions.assertThat(result).isEqualTo(reservationId);

        verify(idempotencyRepository)
                .findByMemberIdAndIdempotencyKey(
                        memberId,
                        idempotencyKey
                );

        verify(idempotencyRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                memberRepository,
                reservationRepository,
                reservationService
        );
    }
}
