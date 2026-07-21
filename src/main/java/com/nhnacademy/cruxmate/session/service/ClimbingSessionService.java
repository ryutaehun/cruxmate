package com.nhnacademy.cruxmate.session.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionResponse;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClimbingSessionService {

    private final ClimbingSessionRepository climbingSessionRepository;

    @Transactional
    public Long createSession(String title,
                              String location,
                              LocalDateTime startAt,
                              LocalDateTime endAt,
                              LocalDateTime reservationOpenAt,
                              LocalDateTime reservationCloseAt,
                              int capacity,
                              ClimbingSessionLevel level){
        ClimbingSession session = ClimbingSession.create(
                title,
                location,
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                capacity,
                level
        );

        ClimbingSession savedSession =
                climbingSessionRepository.save(session);

        return savedSession.getId();
    }

    @Transactional(readOnly = true)
    public ClimbingSessionResponse getSession(Long sessionId){
        ClimbingSession session = climbingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIMBING_SESSION_NOT_FOUND));

        return ClimbingSessionResponse.from(session);
    }
}
