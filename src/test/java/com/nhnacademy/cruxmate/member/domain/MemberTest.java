package com.nhnacademy.cruxmate.member.domain;

import org.junit.jupiter.api.Test;

import static com.nhnacademy.cruxmate.support.TestFixtures.DEFAULT_PASSWORD_HASH;
import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    void 회원을_생성하면_USER_권한을_가진다() {
        Member member = createMember();

        assertThat(member.getEmail()).isEqualTo("member@example.com");
        assertThat(member.getPasswordHash()).isEqualTo(DEFAULT_PASSWORD_HASH);
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }
}
