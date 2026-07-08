package com.nhnacademy.cruxmate.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEM001", "회원을 찾을 수 없습니다."),
    CLIMBING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SES001", "클라이밍 세션을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
