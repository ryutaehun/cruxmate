package com.nhnacademy.cruxmate.auth.dto;

import com.nhnacademy.cruxmate.member.domain.MemberRole;

public record AuthenticatedMember (
        Long memberId,
        String email,
        MemberRole role
){
}
