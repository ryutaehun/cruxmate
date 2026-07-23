package com.nhnacademy.cruxmate.session.repository;

import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClimbingSessionRepository extends JpaRepository<ClimbingSession, Long> {

    // 굳이 @Query를 사용한 이유 -> 단순 조회까지 락을 거는걸 막기 위해서
    @Lock(LockModeType.PESSIMISTIC_WRITE) // 해당 세션 행을 조회한 트랜잭션이 끝날 때까지 다른 트랜잭션의 쓰기용 조회를 기다리게 함
    @Query("""
            select cs from ClimbingSession cs
                        where cs.id = :sessionId
            """)
    Optional<ClimbingSession> findByIdForUpdate(
            @Param("sessionId") Long sessionId
    );

    Page<ClimbingSession> findAllByStatusAndStartAtAfter(
            ClimbingSessionStatus status,
            LocalDateTime startAt,
            Pageable pageable
    );
}
