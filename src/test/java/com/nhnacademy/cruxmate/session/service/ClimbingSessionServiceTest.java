package com.nhnacademy.cruxmate.session.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.dto.PageResponse;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionStatus;
import com.nhnacademy.cruxmate.session.dto.ClimbingSessionResponse;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        ClimbingSessionService.class,
        ClimbingSessionServiceTest.FixedClockConfiguration.class
})
class ClimbingSessionServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 7, 23, 10, 0);

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-07-23T10:00:00Z"),
                    ZoneOffset.UTC
            );
        }
    }

    @Autowired
    private ClimbingSessionService climbingSessionService;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 세션을_생성하면_DB에_저장된다() {
        LocalDateTime now = LocalDateTime.now();

        Long sessionId = climbingSessionService.createSession(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.minusHours(1),
                now.plusDays(1),
                5,
                ClimbingSessionLevel.BEGINNER
        );

        entityManager.flush();
        entityManager.clear();

        ClimbingSession session =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow();

        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(session.getTitle()).isEqualTo("평일 저녁 초보 세션");
        assertThat(session.getLocation()).isEqualTo("광주 온클라이밍");
        assertThat(session.getCapacity()).isEqualTo(5);
        assertThat(session.getReservedCount()).isZero();
        assertThat(session.getLevel()).isEqualTo(ClimbingSessionLevel.BEGINNER);
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getUpdatedAt()).isNotNull();
    }

    @Test
    void 세션을_조회하면_응답_DTO를_반환한다() {
        LocalDateTime now = LocalDateTime.now();

        Long sessionId = climbingSessionService.createSession(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.minusHours(1),
                now.plusDays(1),
                5,
                ClimbingSessionLevel.BEGINNER
        );

        entityManager.flush();
        entityManager.clear();

        ClimbingSessionResponse response =
                climbingSessionService.getSession(sessionId);

        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.title()).isEqualTo("평일 저녁 초보 세션");
        assertThat(response.location()).isEqualTo("광주 온클라이밍");
        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.reservedCount()).isZero();
        assertThat(response.remainingCapacity()).isEqualTo(5);
        assertThat(response.level()).isEqualTo(ClimbingSessionLevel.BEGINNER);
        assertThat(response.status()).isEqualTo(ClimbingSessionStatus.SCHEDULED);
    }

    @Test
    void 존재하지_않는_세션을_조회하면_CLIMBING_SESSION_NOT_FOUND를_던진다() {
        Long nonexistentSessionId = 0L;

        assertThatThrownBy(() ->
                climbingSessionService.getSession(nonexistentSessionId)
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CLIMBING_SESSION_NOT_FOUND)
                );
    }

    @Test
    void 예정된_세션을_시작시간_오름차순으로_조회한다() {
        ClimbingSession pastSession = createSession(
                "이미 시작한 세션",
                NOW.minusHours(3),
                NOW.minusHours(1)
        );

        ClimbingSession startingNowSession = createSession(
                "지금 시작하는 세션",
                NOW,
                NOW.plusHours(2)
        );

        ClimbingSession laterSession = createSession(
                "나중 세션",
                NOW.plusDays(2),
                NOW.plusDays(2).plusHours(2)
        );

        ClimbingSession earlierSession = createSession(
                "먼저 열리는 세션",
                NOW.plusDays(1),
                NOW.plusDays(1).plusHours(2)
        );

        climbingSessionRepository.saveAllAndFlush(
                List.of(
                        pastSession,
                        startingNowSession,
                        laterSession,
                        earlierSession
                )
        );

        PageResponse<ClimbingSessionResponse> response =
                climbingSessionService.getSessions(0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).title())
                .isEqualTo("먼저 열리는 세션");
        assertThat(response.content().get(1).title())
                .isEqualTo("나중 세션");

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void 세션_목록을_페이지_단위로_조회한다() {
        climbingSessionRepository.saveAllAndFlush(List.of(
                createSession("첫 번째 세션", NOW.plusDays(1), NOW.plusDays(1).plusHours(2)),
                createSession("두 번째 세션", NOW.plusDays(2), NOW.plusDays(2).plusHours(2)),
                createSession("세 번째 세션", NOW.plusDays(3), NOW.plusDays(3).plusHours(2))
        ));

        PageResponse<ClimbingSessionResponse> firstPage =
                climbingSessionService.getSessions(0, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        PageResponse<ClimbingSessionResponse> secondPage =
                climbingSessionService.getSessions(1, 2);

        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.page()).isEqualTo(1);
    }

    @Test
    void 페이지_번호가_음수이면_예외가_발생한다() {
        assertThatThrownBy(() -> climbingSessionService.getSessions(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 번호는 0 이상이어야 합니다.");
    }

    @Test
    void 페이지_크기가_1보다_작으면_예외가_발생한다() {
        assertThatThrownBy(() -> climbingSessionService.getSessions(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 크기는 1 이상 100 이하여야 합니다.");
    }

    @Test
    void 페이지_크기가_100보다_크면_예외가_발생한다() {
        assertThatThrownBy(() -> climbingSessionService.getSessions(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 크기는 1 이상 100 이하여야 합니다.");
    }
}
