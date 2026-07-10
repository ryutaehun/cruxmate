package com.nhnacademy.cruxmate.idempotency.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationIdempotencyService {

    private final ReservationIdempotencyRepository idempotencyRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Transactional
    public Long createReservation(
            Long memberId,
            Long sessionId,
            int participantCount,
            String idempotencyKey,
            String requestHash
    ){
        Optional<ReservationIdempotency> existing = idempotencyRepository.findByMemberIdAndIdempotencyKey(
                memberId, idempotencyKey
        );

        if(existing.isPresent()){
            ReservationIdempotency idempotency = existing.get();

            if(idempotency.getRequestHash().equals(requestHash) && idempotency.getStatus() == IdempotencyStatus.COMPLETED){
                return idempotency.getReservation().getId();
            }
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ReservationIdempotency idempotency = ReservationIdempotency.create(
                member,
                idempotencyKey,
                requestHash
        );

        idempotencyRepository.saveAndFlush(idempotency);

        Long reservationId = reservationService.createReservation(
                memberId,
                sessionId,
                participantCount
        );

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        idempotency.complete(reservation, LocalDateTime.now());

        return reservationId;
    }
}
