package com.nhnacademy.cruxmate.reservation.repository;

import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        select r.session.id
        from Reservation r
        where r.id = :reservationId
          and r.member.id = :memberId
        """)
    Optional<Long> findSessionIdByIdAndMemberId(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Reservation r
        where r.id = :reservationId
          and r.member.id = :memberId
        """)
    Optional<Reservation> findByIdAndMemberIdForUpdate(
            @Param("reservationId") Long reservationId,
            @Param("memberId") Long memberId
    );

    boolean existsByMember_IdAndSession_IdAndStatus(Long memberId, Long sessionId, ReservationStatus status);

    long countBySession_Id(Long sessionId);
}
