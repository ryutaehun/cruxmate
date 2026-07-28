package com.nhnacademy.cruxmate.reservation.controller;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.auth.dto.AuthenticatedMember;
import com.nhnacademy.cruxmate.common.security.jwt.JwtTokenService;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.domain.MemberRole;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void 인증된_회원은_자신의_예약만_조회한다()
            throws Exception {

        Member memberA =
                memberRepository.saveAndFlush(
                        createMember("reservation-list-a@example.com")
                );

        Member memberB =
                memberRepository.saveAndFlush(
                        createMember("reservation-list-b@example.com")
                );

        LocalDateTime now = LocalDateTime.now();

        ClimbingSession sessionA =
                climbingSessionRepository.saveAndFlush(
                        createSession(
                                "회원 A의 세션",
                                now.plusDays(2),
                                now.plusDays(2).plusHours(2)
                        )
                );

        ClimbingSession sessionB =
                climbingSessionRepository.saveAndFlush(
                        createSession(
                                "회원 B의 세션",
                                now.plusDays(3),
                                now.plusDays(3).plusHours(2)
                        )
                );

        Reservation reservationA =
                reservationRepository.saveAndFlush(
                        Reservation.create(
                                memberA,
                                sessionA,
                                2
                        )
                );

        reservationRepository.saveAndFlush(
                Reservation.create(
                        memberB,
                        sessionB,
                        1
                )
        );

        String accessToken = issueToken(memberA);

        mockMvc.perform(
                        get("/api/reservations/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].reservationId")
                        .value(reservationA.getId()))
                .andExpect(jsonPath("$.content[0].participantCount")
                        .value(2))
                .andExpect(jsonPath("$.content[0].status")
                        .value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].sessionId")
                        .value(sessionA.getId()))
                .andExpect(jsonPath("$.content[0].sessionTitle")
                        .value("회원 A의 세션"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }

    @Test
    void 인증된_회원은_자신의_예약을_취소할_수_있다()
            throws Exception {

        Member member =
                memberRepository.saveAndFlush(
                        createMember("reservation-cancel-api@example.com")
                );

        LocalDateTime now = LocalDateTime.now();

        ClimbingSession session =
                climbingSessionRepository.saveAndFlush(
                        createSession(
                                now.plusDays(2),
                                now.plusDays(2).plusHours(2),
                                now.minusDays(1),
                                now.plusDays(1),
                                5
                        )
                );

        Long reservationId =
                reservationService.createReservation(
                        member.getId(),
                        session.getId(),
                        2
                );

        String accessToken = issueToken(member);

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/cancel",
                                reservationId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId")
                        .value(reservationId));

        Reservation canceledReservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow();

        ClimbingSession updatedSession =
                climbingSessionRepository.findById(session.getId())
                        .orElseThrow();

        assertThat(canceledReservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELED);

        assertThat(canceledReservation.getCanceledAt())
                .isNotNull();

        assertThat(updatedSession.getReservedCount())
                .isZero();
    }

    @Test
    void 다른_회원의_예약을_취소하면_404를_반환한다()
            throws Exception {

        Member owner =
                memberRepository.saveAndFlush(
                        createMember("reservation-owner@example.com")
                );

        Member otherMember =
                memberRepository.saveAndFlush(
                        createMember("reservation-other@example.com")
                );

        LocalDateTime now = LocalDateTime.now();

        ClimbingSession session =
                climbingSessionRepository.saveAndFlush(
                        createSession(
                                now.plusDays(2),
                                now.plusDays(2).plusHours(2),
                                now.minusDays(1),
                                now.plusDays(1),
                                5
                        )
                );

        Long reservationId =
                reservationService.createReservation(
                        owner.getId(),
                        session.getId(),
                        2
                );

        String otherMemberToken =
                issueToken(otherMember);

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/cancel",
                                reservationId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + otherMemberToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RES002"));

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow();

        ClimbingSession unchangedSession =
                climbingSessionRepository.findById(session.getId())
                        .orElseThrow();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);

        assertThat(unchangedSession.getReservedCount())
                .isEqualTo(2);
    }

    @Test
    void 토큰_없이_예약을_취소하면_401을_반환한다()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/cancel",
                                1L
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH002"));
    }

    private String issueToken(Member member) {
        return jwtTokenService.issueAccessToken(
                new AuthenticatedMember(
                        member.getId(),
                        member.getEmail(),
                        MemberRole.USER
                )
        );
    }
}