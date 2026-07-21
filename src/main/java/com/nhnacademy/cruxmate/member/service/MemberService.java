package com.nhnacademy.cruxmate.member.service;

import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.dto.MemberCreateResponse;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberCreateResponse createMember(String email, String password){
        if(memberRepository.existsByEmail(email)){
            throw new BusinessException(ErrorCode.DUPLICATE_MEMBER_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.create(email, encodedPassword);

        Member savedMember = memberRepository.save(member);

        return new MemberCreateResponse(savedMember.getId(), savedMember.getEmail());
    }
}
