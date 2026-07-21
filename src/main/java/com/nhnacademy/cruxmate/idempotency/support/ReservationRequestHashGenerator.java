package com.nhnacademy.cruxmate.idempotency.support;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ReservationRequestHashGenerator {

    public String generate(Long memberId, Long sessionId, int participantCount){
        String source = memberId + ":" + sessionId + ":" + participantCount;
        return sha256(source);
    }

    // 단방향 암호화 해시 알고리즘 64자리의 16진수 문자열로 변환
    private String sha256(String source){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(source.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for(byte b : encodedHash){
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 알고리즘 사용할 수 없습니다.");
        }
    }
}
