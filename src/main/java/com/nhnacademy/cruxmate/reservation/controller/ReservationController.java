package com.nhnacademy.cruxmate.reservation.controller;

import com.nhnacademy.cruxmate.idempotency.facade.ReservationIdempotencyFacade;
import com.nhnacademy.cruxmate.idempotency.support.ReservationRequestHashGenerator;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateRequest;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationIdempotencyFacade reservationIdempotencyFacade;
    private final ReservationRequestHashGenerator requestHashGenerator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationCreateResponse createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        Long memberId = Long.valueOf(jwt.getSubject());

        String requestHash =
                requestHashGenerator.generate(
                        memberId,
                        request.sessionId(),
                        request.participantCount()
                );

        Long reservationId =
                reservationIdempotencyFacade.createReservation(
                        memberId,
                        request.sessionId(),
                        request.participantCount(),
                        idempotencyKey,
                        requestHash
                );

        return new ReservationCreateResponse(reservationId);
    }
}
