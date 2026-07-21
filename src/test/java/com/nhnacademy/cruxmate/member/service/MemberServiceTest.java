package com.nhnacademy.cruxmate.member.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.config.PasswordConfig;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.domain.MemberRole;
import com.nhnacademy.cruxmate.member.dto.MemberCreateResponse;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        MemberService.class,
        PasswordConfig.class
})
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;


    @Test
    void 회원을_생성하면_암호화된_비밀번호와_USER_권한으로_저장된다() {
        MemberCreateResponse response = memberService.createMember(
                "member@example.com",
                "password123!"
        );

        entityManager.flush();
        entityManager.clear();

        Member member = memberRepository.findById(response.memberId())
                .orElseThrow();

        assertThat(member.getEmail())
                .isEqualTo("member@example.com");

        assertThat(member.getRole())
                .isEqualTo(MemberRole.USER);

        assertThat(member.getPasswordHash())
                .isNotEqualTo("password123!");

        assertThat(passwordEncoder.matches(
                "password123!",
                member.getPasswordHash()
        )).isTrue();
    }

    @Test
    void 이미_사용중인_이메일이면_회원생성에_실패한다() {
        memberService.createMember(
                "member@example.com",
                "password123!"
        );

        assertThatThrownBy(() ->
                memberService.createMember(
                        "member@example.com",
                        "anotherPassword123!"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    ErrorCode.DUPLICATE_MEMBER_EMAIL
                            );
                });

        assertThat(memberRepository.count()).isEqualTo(1);
    }
}
