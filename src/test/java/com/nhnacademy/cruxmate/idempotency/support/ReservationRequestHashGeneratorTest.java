package com.nhnacademy.cruxmate.idempotency.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationRequestHashGeneratorTest {

    private final ReservationRequestHashGenerator generator = new ReservationRequestHashGenerator();

    @Test
    void 같은_예약_요청이면_같은_해시를_생성한다(){
        String first = generator.generate(1L, 10L, 2);
        String second = generator.generate(1L, 10L, 2);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void 참가_인원이_다르면_다른_해시를_생성한다() {
        String first = generator.generate(1L, 10L, 1);
        String second = generator.generate(1L, 10L, 2);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 세션이_다르면_다른_해시를_생성한다() {
        String first = generator.generate(1L, 10L, 2);
        String second = generator.generate(1L, 11L, 2);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 회원이_다르면_다른_해시를_생성한다() {
        String first = generator.generate(1L, 10L, 2);
        String second = generator.generate(2L, 10L, 2);

        assertThat(first).isNotEqualTo(second);
    }
}
