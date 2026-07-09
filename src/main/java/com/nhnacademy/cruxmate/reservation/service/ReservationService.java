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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ClimbingSessionRepository climbingSessionRepository;

    @Transactional
    public Long createReservation(
            Long memberId,
            Long sessionId,
            int participantCount
    ){
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );
        ClimbingSession climbingSession = climbingSessionRepository.findByIdForUpdate(sessionId).orElseThrow(
                () -> new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND)
        );

        boolean alreadyReserved =
                reservationRepository.existsByMember_IdAndSession_IdAndStatus(
                        memberId, sessionId, ReservationStatus.CONFIRMED
                );

        if(alreadyReserved){
            throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION);
        }

        climbingSession.reserve(participantCount, LocalDateTime.now());

        Reservation reservation = Reservation.create(member, climbingSession, participantCount);

        Reservation savedReservation = reservationRepository.save(reservation);

        return savedReservation.getId();
    }

    @Transactional
    public Long cancelReservation(Long memberId, Long reservationId){
        Long sessionId = reservationRepository.findSessionIdByIdAndMemberId(reservationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        ClimbingSession session = climbingSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND));
        Reservation reservation = reservationRepository.findByIdAndMemberIdForUpdate(reservationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        reservation.cancel(now);
        session.release(reservation.getParticipantCount());

        return reservationId;
    }
}
