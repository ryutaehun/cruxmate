package com.nhnacademy.cruxmate.reservation.controller;

import com.nhnacademy.cruxmate.idempotency.service.ReservationIdempotencyService;
import com.nhnacademy.cruxmate.idempotency.support.ReservationRequestHashGenerator;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateRequest;
import com.nhnacademy.cruxmate.reservation.dto.ReservationCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationIdempotencyService idempotencyService;
    private final ReservationRequestHashGenerator requestHashGenerator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationCreateResponse createReservation(@RequestHeader("X-MEMBER-ID") Long memberId,
                                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                       @Valid @RequestBody ReservationCreateRequest request){
        String requestHash = requestHashGenerator.generate(memberId, request.sessionId(), request.participantCount());

        Long reservationId = idempotencyService.createReservation(memberId, request.sessionId(), request.participantCount(), idempotencyKey, requestHash);

        return new ReservationCreateResponse(reservationId);
    }
}
