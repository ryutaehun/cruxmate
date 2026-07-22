package com.nhnacademy.cruxmate.idempotency.concurrency;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.idempotency.domain.IdempotencyStatus;
import com.nhnacademy.cruxmate.idempotency.domain.ReservationIdempotency;
import com.nhnacademy.cruxmate.idempotency.facade.ReservationIdempotencyFacade;
import com.nhnacademy.cruxmate.idempotency.repository.ReservationIdempotencyRepository;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@Slf4j
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReservationIdempotencyConcurrencyTest {

    @Autowired
    private ReservationIdempotencyFacade reservationIdempotencyFacade;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockitoSpyBean
    private ReservationIdempotencyRepository idempotencyRepository;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Test
    void 동일한_멱등성키_요청이_동시에_들어와도_같은_예약결과를_반환한다()
            throws Exception {

        Member member = createMember(
                "idempotency-concurrency@example.com"
        );
        memberRepository.saveAndFlush(member);

        LocalDateTime now = LocalDateTime.now();

        ClimbingSession session = createSession(
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.minusDays(1),
                now.plusDays(1),
                5
        );
        climbingSessionRepository.saveAndFlush(session);

        Long memberId = member.getId();
        Long sessionId = session.getId();
        int participantCount = 2;

        String idempotencyKey =
                "same-concurrent-idempotency-key";

        String requestHash =
                memberId + ":" + sessionId + ":" + participantCount;

        CountDownLatch lookupLatch = new CountDownLatch(2);
        AtomicInteger lookupCount = new AtomicInteger();

        doAnswer(invocation -> {
            Long requestedMemberId =
                    invocation.getArgument(0, Long.class);

            String requestedIdempotencyKey =
                    invocation.getArgument(1, String.class);

            Optional<ReservationIdempotency> result;

            try (EntityManager entityManager =
                         entityManagerFactory.createEntityManager()) {
                result = entityManager.createQuery(
                                """
                                select idempotency
                                from ReservationIdempotency idempotency
                                where idempotency.member.id = :memberId
                                  and idempotency.idempotencyKey = :idempotencyKey
                                """,
                                ReservationIdempotency.class
                        )
                        .setParameter("memberId", requestedMemberId)
                        .setParameter(
                                "idempotencyKey",
                                requestedIdempotencyKey
                        )
                        .getResultList()
                        .stream()
                        .findFirst();
            }

            int currentCall = lookupCount.incrementAndGet();

            if (currentCall <= 2) {
                lookupLatch.countDown();

                boolean bothRequestsLookedUp =
                        lookupLatch.await(5, TimeUnit.SECONDS);

                if (!bothRequestsLookedUp) {
                    throw new IllegalStateException(
                            "두 요청이 멱등성 조회 지점에 도달하지 못했습니다."
                    );
                }
            }

            return result;
        }).when(idempotencyRepository)
                .findByMemberIdAndIdempotencyKey(
                        memberId,
                        idempotencyKey
                );

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        ConcurrentLinkedQueue<Long> reservationIds =
                new ConcurrentLinkedQueue<>();

        ConcurrentLinkedQueue<Throwable> failures =
                new ConcurrentLinkedQueue<>();

        Runnable task = () -> {
            try {
                startLatch.await();

                Long reservationId =
                        reservationIdempotencyFacade.createReservation(
                                memberId,
                                sessionId,
                                participantCount,
                                idempotencyKey,
                                requestHash
                        );

                reservationIds.add(reservationId);

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);

            } catch (Throwable throwable) {
                failures.add(throwable);

            } finally {
                doneLatch.countDown();
            }
        };

        Thread firstThread = new Thread(
                task,
                "idempotency-request-1"
        );

        Thread secondThread = new Thread(
                task,
                "idempotency-request-2"
        );

        firstThread.start();
        secondThread.start();

        startLatch.countDown();

        boolean completed =
                doneLatch.await(10, TimeUnit.SECONDS);

        log.info("completed = {}", completed);
        log.info("reservationIds = {}", reservationIds);
        log.info("failureCount = {}", failures.size());

        failures.forEach(failure ->
                log.info(
                        "idempotency concurrency failure",
                        failure
                )
        );

        assertThat(completed).isTrue();

        assertThat(failures).isEmpty();

        assertThat(reservationIds).hasSize(2);

        assertThat(
                reservationIds.stream()
                        .distinct()
                        .toList()
        ).hasSize(1);

        assertThat(
                reservationRepository.countBySession_Id(sessionId)
        ).isEqualTo(1);

        ClimbingSession savedSession =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow();

        assertThat(savedSession.getReservedCount())
                .isEqualTo(participantCount);

        Optional<ReservationIdempotency> savedIdempotency =
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                memberId,
                                idempotencyKey
                        );

        assertThat(savedIdempotency).isPresent();

        assertThat(savedIdempotency.orElseThrow().getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
    }
}
