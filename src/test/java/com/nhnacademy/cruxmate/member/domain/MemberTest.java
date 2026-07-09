package com.nhnacademy.cruxmate.member.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberTest {

    @Test
    void 정상_생성_확인(){
        Member member = Member.create("1234@gmail.com", "1234");

        assertThat(member.getEmail()).isEqualTo("1234@gmail.com");
        assertThat(member.getPasswordHash()).isEqualTo("1234");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }
}
