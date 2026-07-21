package com.nhnacademy.cruxmate.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COM001", "요청 값이 올바르지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEM001", "회원을 찾을 수 없습니다."),
    DUPLICATE_MEMBER_EMAIL(HttpStatus.CONFLICT, "MEM002", "이미 사용 중인 이메일입니다."),
    CLIMBING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SES001", "클라이밍 세션을 찾을 수 없습니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "RES001", "이미 해당 세션을 예약했습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RES002", "예약을 찾을 수 없습니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "IDM001", "동일한 Idempotency-Key를 다른 요청에 사용할 수 없습니다."),
    IDEMPOTENCY_REQUEST_PROCESSING(HttpStatus.CONFLICT, "IDM002", "이미 처리 중인 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
