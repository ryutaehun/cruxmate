package com.nhnacademy.cruxmate.reservation.controller;

import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.idempotency.facade.ReservationIdempotencyFacade;
import com.nhnacademy.cruxmate.idempotency.support.ReservationRequestHashGenerator;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCancelResponse;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateRequest;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateResponse;
import com.nhnacademy.cruxmate.reservation.dto.ReservationResponse;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
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
    private final ReservationService reservationService;

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

    @GetMapping("/me")
    public PageResponse<ReservationResponse> getMyReservations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Long memberId = Long.valueOf(jwt.getSubject());

        return reservationService.getMyReservations(memberId, page, size);
    }

    @PatchMapping("/{reservationId}/cancel")
    public ReservationCancelResponse cancelReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reservationId
    ){
        Long memberId = Long.valueOf(jwt.getSubject());

        Long cancelReservationId = reservationService.cancelReservation(memberId, reservationId);

        return new ReservationCancelResponse(cancelReservationId);
    }
}
