package com.nhnacademy.cruxmate.member.controller;

import com.nhnacademy.cruxmate.member.dto.MemberCreateRequest;
import com.nhnacademy.cruxmate.member.dto.MemberCreateResponse;
import com.nhnacademy.cruxmate.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberCreateResponse createMember(@Valid @RequestBody MemberCreateRequest request){
        return memberService.createMember(request.email(), request.password());
    }
}
