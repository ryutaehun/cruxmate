package com.nhnacademy.cruxmate.idempotency.facade;

import com.nhnacademy.cruxmate.idempotency.service.ReservationIdempotencyService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationIdempotencyFacade {

    private static final String IDEMPOTENCY_UNIQUE_CONSTRAINT =
            "uk_reservation_idempotency_member_key";

    private final ReservationIdempotencyService idempotencyService;

    public Long createReservation(
            Long memberId,
            Long sessionId,
            int participantCount,
            String idempotencyKey,
            String requestHash
    ) {
        try {
            return idempotencyService.createReservation(
                    memberId,
                    sessionId,
                    participantCount,
                    idempotencyKey,
                    requestHash
            );

        } catch (DataIntegrityViolationException exception) {
            if (!isIdempotencyKeyConstraintViolation(exception)) {
                throw exception;
            }

            return idempotencyService.getExistingReservationResult(
                    memberId,
                    idempotencyKey,
                    requestHash
            );
        }
    }

    private boolean isIdempotencyKeyConstraintViolation(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ConstraintViolationException exception) {
                String constraintName = exception.getConstraintName();

                return constraintName != null
                        && constraintName.endsWith(
                        IDEMPOTENCY_UNIQUE_CONSTRAINT
                );
            }

            current = current.getCause();
        }

        return false;
    }
}
