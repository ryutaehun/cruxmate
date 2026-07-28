package com.nhnacademy.cruxmate.reservation.service;

import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.dto.ReservationResponse;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ClimbingSessionRepository climbingSessionRepository;
    private final Clock clock;

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

        climbingSession.reserve(participantCount, LocalDateTime.now(clock));

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

        LocalDateTime now = LocalDateTime.now(clock);

        reservation.cancel(now);
        session.release(reservation.getParticipantCount());

        return reservationId;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getMyReservations(Long memberId, int page, int size){
        if(page < 0){
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if(size < 1 || size > 100){
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReservationResponse> result = reservationRepository
                .findAllByMember_Id(memberId, pageable).map(ReservationResponse::from);

        return PageResponse.from(result);

    }
}
