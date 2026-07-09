package com.nhnacademy.cruxmate.reservation.concurrency;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import com.nhnacademy.cruxmate.reservation.repository.ReservationRepository;
import com.nhnacademy.cruxmate.reservation.service.ReservationService;
import com.nhnacademy.cruxmate.session.domain.ClimbingSession;
import com.nhnacademy.cruxmate.session.domain.ClimbingSessionLevel;
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

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest
@Import({TestcontainersConfiguration.class})
public class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClimbingSessionRepository climbingSessionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void 마지막_한_자리에_두_회원이_동시에_예약한다() throws Exception{
        Member member1 = Member.create("abc@naver.com", "1234");
        Member member2 = Member.create("def@naver.com", "5678");
        memberRepository.save(member1);
        memberRepository.save(member2);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationOpenAt = now.minusDays(1);
        LocalDateTime reservationCloseAt = now.plusDays(1);
        LocalDateTime startAt = now.plusDays(2);
        LocalDateTime endAt = now.plusDays(2).plusHours(2);

        ClimbingSession session = ClimbingSession.create(
                "평일 저녁 초보 세션",
                "광주 온클라이밍",
                startAt,
                endAt,
                reservationOpenAt,
                reservationCloseAt,
                1,
                ClimbingSessionLevel.BEGINNER
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

        assertThat(reservationRepository.count()).isEqualTo(1);

        ClimbingSession savedSession =
                climbingSessionRepository.findById(sessionId)
                        .orElseThrow();

        assertThat(savedSession.getReservedCount()).isEqualTo(1);
    }
}
