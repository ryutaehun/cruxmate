package com.nhnacademy.cruxmate.session.controller;

import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionCreateRequest;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionCreateResponse;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionResponse;
import com.nhnacademy.cruxmate.session.service.ClimbingSessionService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody ClimbingSessionCreateRequest request
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

    @GetMapping("/{sessionId}")
    public ClimbingSessionResponse getSession(@PathVariable Long sessionId){
        return climbingSessionService.getSession(sessionId);
    }

    @GetMapping
    public PageResponse<ClimbingSessionResponse> getSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return climbingSessionService.getSessions(page, size);
    }
}
