package com.nhnacademy.cruxmate.reservation.concurrency;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.common.exception.BusinessException;
import com.nhnacademy.cruxmate.common.exception.ErrorCode;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.domain.Reservation;
import com.nhnacademy.cruxmate.reservation.domain.ReservationStatus;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.repository.ClimbingSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static com.nhnacademy.cruxmate.support.TestFixtures.createSession;
import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest
@Import({TestcontainersConfiguration.class})
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void 마지막_한_자리에_두_회원이_동시에_예약하면_하나만_성공한다() throws Exception {
        Member member1 = createMember("concurrency-first@example.com");
        Member member2 = createMember("concurrency-second@example.com");
        memberRepository.save(member1);
        memberRepository.save(member2);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationOpenAt = now.minusDays(1);
        LocalDateTime reservationCloseAt = now.plusDays(1);
        LocalDateTime startAt = now.plusDays(2);
        LocalDateTime endAt = now.plusDays(2).plusHours(2);

        ClimbingSession session = createSession(
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                1
        );
        climbingSessionRepository.save(session);

        Long member1Id = member1.getId();
        Long member2Id = member2.getId();
        Long sessionId = session.getId();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures =
                new ConcurrentLinkedQueue<>();

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();

                reservationService.createReservation(
                        member1Id,
                        sessionId,
                        1
                );

                successCount.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();

                reservationService.createReservation(
                        member2Id,
                        sessionId,
                        1
                );

                successCount.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        startLatch.countDown();

        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

        log.info("successCount = {}",successCount.get());
        log.info("failuresSize = {}",failures.size());

        failures.forEach(failure ->
                log.info("reservation failure", failure)
        );

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures)
                .hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능 인원이 초과되었습니다");

        assertThat(reservationRepository.countBySession_Id(sessionId)).isEqualTo(1);

        ClimbingSession savedSession =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow();

        assertThat(savedSession.getReservedCount()).isEqualTo(1);
    }

    @Test
    void 같은_예약을_동시에_취소해도_인원이_한번만_감소한다() throws Exception {
        Member member = createMember("concurrency-cancel@example.com");
        memberRepository.save(member);

        Long memberId = member.getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationOpenAt = now.minusDays(1);
        LocalDateTime reservationCloseAt = now.plusDays(1);
        LocalDateTime startAt = now.plusDays(2);
        LocalDateTime endAt = now.plusDays(2).plusHours(2);

        ClimbingSession session = createSession(
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                4
        );
        climbingSessionRepository.save(session);

        Long sessionId = session.getId();

        Long reservationId = reservationService.createReservation(memberId, sessionId, 2);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures =
                new ConcurrentLinkedQueue<>();

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();

                reservationService.cancelReservation(memberId, reservationId);

                successCount.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();

                reservationService.cancelReservation(memberId, reservationId);

                successCount.incrementAndGet();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                doneLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        startLatch.countDown();

        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

        log.info("cancelSuccessCount = {}", successCount.get());
        log.info("cancelFailuresSize = {}", failures.size());

        failures.forEach(failure ->
                log.info("cancel failure", failure)
        );

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures)
                .hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");

        Reservation savedReservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
                        );

        ClimbingSession savedSession =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CLIMBING_SESSION_NOT_FOUND
                                )
                        );

        assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(savedReservation.getCanceledAt()).isNotNull();
        assertThat(savedSession.getReservedCount()).isZero();
        assertThat(reservationRepository.countBySession_Id(sessionId)).isEqualTo(1);
    }
}
