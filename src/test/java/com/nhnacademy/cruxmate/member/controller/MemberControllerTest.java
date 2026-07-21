package com.nhnacademy.cruxmate.member.controller;

import com.nhnacademy.cruxmate.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    void 회원생성_요청값이_유효하지_않으면_400을_반환한다() throws Exception {
        String request = """
                {
                  "email": "invalid-email",
                  "password": "1234"
                }
                """;

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"))
                .andExpect(jsonPath("$.errors.email")
                        .value("올바른 이메일 형식이어야 합니다."))
                .andExpect(jsonPath("$.errors.password")
                        .value("비밀번호는 8자 이상 50자 이하여야 합니다."));
    }
}