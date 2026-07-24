package com.nhnacademy.cruxmate.auth.controller;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.auth.dto.AuthenticatedMember;
import com.nhnacademy.cruxmate.auth.dto.LoginResponse;
import com.nhnacademy.cruxmate.common.security.jwt.JwtTokenService;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.domain.MemberRole;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 로그인에_성공하면_AccessToken을_반환한다()
            throws Exception {

        Member member = Member.create(
                "jwt-login@example.com",
                passwordEncoder.encode("password123!")
        );
        memberRepository.saveAndFlush(member);

        String request = """
                {
                  "email": "jwt-login@example.com",
                  "password": "password123!"
                }
                """;

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(3600))
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        Jwt jwt = jwtDecoder.decode(
                response.accessToken()
        );

        assertThat(jwt.getSubject())
                .isEqualTo(member.getId().toString());

        assertThat(jwt.getClaimAsString("email"))
                .isEqualTo("jwt-login@example.com");

        assertThat(jwt.getClaimAsString("role"))
                .isEqualTo("USER");

        assertThat(jwt.getIssuer().toString())
                .isEqualTo("https://cruxmate.local");
    }
    @Test
    void 비밀번호가_틀리면_401을_반환한다()
            throws Exception {

        Member member = Member.create(
                "jwt-failure@example.com",
                passwordEncoder.encode("password123!")
        );
        memberRepository.saveAndFlush(member);

        String request = """
            {
              "email": "jwt-failure@example.com",
              "password": "wrong-password"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH001"));
    }

    @Test
    void 토큰_없이_예약을_요청하면_401을_반환한다()
            throws Exception {

        String request = """
            {
              "sessionId": 1,
              "participantCount": 1
            }
            """;

        mockMvc.perform(
                        post("/api/reservations")
                                .header(
                                        "Idempotency-Key",
                                        "security-test-key"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH002"));
    }

    @Test
    void 잘못된_토큰으로_예약을_요청하면_401을_반환한다()
            throws Exception {

        String request = """
            {
              "sessionId": 1,
              "participantCount": 1
            }
            """;

        mockMvc.perform(
                        post("/api/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token"
                                )
                                .header(
                                        "Idempotency-Key",
                                        "invalid-token-test-key"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH002"));
    }

    @Test
    void USER_권한으로_세션을_생성하면_403을_반환한다()
            throws Exception {

        String accessToken = jwtTokenService.issueAccessToken(
                new AuthenticatedMember(
                        1L,
                        "user@example.com",
                        MemberRole.USER
                )
        );

        String request = """
            {
              "title": "금요일 저녁 클라이밍",
              "location": "클라이밍장",
              "startAt": "2026-08-01T19:00:00",
              "endAt": "2026-08-01T21:00:00",
              "reservationOpenAt": "2026-07-25T09:00:00",
              "reservationCloseAt": "2026-07-31T18:00:00",
              "capacity": 10,
              "level": "BEGINNER"
            }
            """;

        mockMvc.perform(
                        post("/api/sessions")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("AUTH003"));
    }
}
