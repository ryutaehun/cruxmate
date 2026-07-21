package com.nhnacademy.cruxmate.session.service;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        ClimbingSessionService.class
})
class ClimbingSessionServiceTest {

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
}