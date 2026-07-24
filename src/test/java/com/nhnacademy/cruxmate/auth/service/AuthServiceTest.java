package com.nhnacademy.cruxmate.auth.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.auth.dto.AuthenticatedMember;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.domain.MemberRole;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 올바른_이메일과_비밀번호로_인증한다() {
        Member member = Member.create(
                "login-success@example.com",
                passwordEncoder.encode("password123!")
        );
        memberRepository.saveAndFlush(member);

        AuthenticatedMember result = authService.authenticate(
                "login-success@example.com",
                "password123!"
        );

        assertThat(result.memberId()).isEqualTo(member.getId());
        assertThat(result.email())
                .isEqualTo("login-success@example.com");
        assertThat(result.role()).isEqualTo(MemberRole.USER);
    }

    @Test
    void 비밀번호가_일치하지_않으면_인증에_실패한다() {
        Member member = Member.create(
                "login-failure@example.com",
                passwordEncoder.encode("password123!")
        );
        memberRepository.saveAndFlush(member);

        assertThatThrownBy(() ->
                authService.authenticate(
                        "login-failure@example.com",
                        "wrong-password"
                )
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void 존재하지_않는_이메일이면_인증에_실패한다() {
        assertThatThrownBy(() ->
                authService.authenticate(
                        "unknown@example.com",
                        "password123!"
                )
        ).isInstanceOf(BadCredentialsException.class);
    }
}