package com.nhnacademy.cruxmate.auth.dto;

public record LoginResponse (
        String accessToken,
        String tokenType,
        long expiresIn
){
}
