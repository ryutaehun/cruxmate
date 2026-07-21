package com.nhnacademy.cruxmate.session.controller;

import com.nhnacademy.cruxmate.session.dto.ClimbingSessionCreateRequest;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionCreateResponse;
import com.nhnacademy.cruxmate.session.service.ClimbingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class ClimbingSessionController {

    private final ClimbingSessionService climbingSessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClimbingSessionCreateResponse createSession(
            @RequestBody ClimbingSessionCreateRequest request
    ) {
        Long sessionId = climbingSessionService.createSession(
                request.title(),
                request.location(),
                request.startAt(),
                request.endAt(),
                request.reservationOpenAt(),
                request.reservationCloseAt(),
                request.capacity(),
                request.level()
        );

        return new ClimbingSessionCreateResponse(sessionId);
    }
}
