package com.nhnacademy.cruxmate.member.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemberTest {

    @Test
    void 정상_생성_확인(){
        Member member = Member.create("1234@gmail.com", "1234");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1234@gmail.com", member.getEmail()),
                () -> Assertions.assertEquals("1234", member.getPasswordHash()),
                () -> Assertions.assertEquals(MemberRole.USER, member.getRole())
        );
    }
}
