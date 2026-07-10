package com.nhnacademy.cruxmate.idempotency.repository;

import com.nhnacademy.cruxmate.TestcontainersConfiguration;
import com.nhnacademy.cruxmate.idempotency.domain.IdempotencyStatus;
import com.nhnacademy.cruxmate.idempotency.domain.ReservationIdempotency;
import com.nhnacademy.cruxmate.member.domain.Member;
import com.nhnacademy.cruxmate.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static com.nhnacademy.cruxmate.support.TestFixtures.createMember;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ReservationIdempotencyRepositoryTest {

    @Autowired
    private ReservationIdempotencyRepository idempotencyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 멱등성_요청을_저장하고_회원과_키로_조회한다() {
        Member member = createMember("idempotency@example.com");
        memberRepository.save(member);

        ReservationIdempotency idempotency =
                ReservationIdempotency.create(
                        member,
                        "reservation-key-123",
                        "a".repeat(64)
                );

        idempotencyRepository.save(idempotency);

        Long idempotencyId = idempotency.getId();
        Long memberId = member.getId();

        entityManager.flush();
        entityManager.clear();

        ReservationIdempotency found =
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                memberId,
                                "reservation-key-123"
                        )
                        .orElseThrow();

        assertThat(found.getId()).isEqualTo(idempotencyId);
        assertThat(found.getMember().getId()).isEqualTo(memberId);
        assertThat(found.getIdempotencyKey())
                .isEqualTo("reservation-key-123");
        assertThat(found.getRequestHash())
                .isEqualTo("a".repeat(64));
        assertThat(found.getStatus())
                .isEqualTo(IdempotencyStatus.PROCESSING);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCompletedAt()).isNull();
        assertThat(found.getReservation()).isNull();
    }

    @Test
    void 같은_회원과_같은_멱등성_키는_두_번_저장할_수_없다() {
        Member member = createMember("duplicate-idempotency@example.com");
        memberRepository.save(member);

        ReservationIdempotency first =
                ReservationIdempotency.create(
                        member,
                        "same-key",
                        "a".repeat(64)
                );

        ReservationIdempotency second =
                ReservationIdempotency.create(
                        member,
                        "same-key",
                        "a".repeat(64)
                );

        idempotencyRepository.save(first);
        entityManager.flush();

        assertThatThrownBy(() -> idempotencyRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_회원은_같은_멱등성_키를_사용할_수_있다() {
        Member firstMember = createMember("first-idempotency@example.com");
        Member secondMember = createMember("second-idempotency@example.com");

        memberRepository.save(firstMember);
        memberRepository.save(secondMember);

        ReservationIdempotency first =
                ReservationIdempotency.create(
                        firstMember,
                        "same-key",
                        "a".repeat(64)
                );

        ReservationIdempotency second =
                ReservationIdempotency.create(
                        secondMember,
                        "same-key",
                        "a".repeat(64)
                );

        idempotencyRepository.save(first);
        idempotencyRepository.save(second);

        entityManager.flush();
        entityManager.clear();

        assertThat(
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                firstMember.getId(),
                                "same-key"
                        )
        ).isPresent();

        assertThat(
                idempotencyRepository
                        .findByMemberIdAndIdempotencyKey(
                                secondMember.getId(),
                                "same-key"
                        )
        ).isPresent();
    }
}
