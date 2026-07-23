package com.nhnacademy.cruxmate.session.service;

import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionStatus;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionResponse;
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
public class ClimbingSessionService {

    private final ClimbingSessionRepository climbingSessionRepository;
    private final Clock clock;

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

    @Transactional(readOnly = true)
    public PageResponse<ClimbingSessionResponse> getSessions(int page, int size){
        if(page < 0){
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if(size < 1 || size > 100){
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startAt"));

        Page<ClimbingSessionResponse> result = climbingSessionRepository
                .findAllByStatusAndStartAtAfter(ClimbingSessionStatus.SCHEDULED, LocalDateTime.now(clock), pageable)
                .map(ClimbingSessionResponse::from);

        return PageResponse.from(result);
    }
}
