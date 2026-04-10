package com.lhcsim.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RandomServiceTest {

    @Test
    void testDeterministic() {
        RandomService rs1 = new RandomService(12345L);
        RandomService rs2 = new RandomService(12345L);

        for (int i = 0; i < 100; i++) {
            assertThat(rs1.nextDouble(RandomService.EVENTS))
                    .isEqualTo(rs2.nextDouble(RandomService.EVENTS));
        }
    }

    @Test
    void testIndependentStreams() {
        RandomService rs = new RandomService(42L);
        double v1 = rs.nextDouble(RandomService.EVENTS);
        // Reseed to reset
        rs.reseed(42L);
        double v2 = rs.nextDouble(RandomService.OPTICS);

        assertThat(v1).isNotEqualTo(v2);
    }

    @Test
    void testPoissonNonNegative() {
        RandomService rs = new RandomService(99L);
        for (int i = 0; i < 1000; i++) {
            assertThat(rs.nextPoisson(RandomService.EVENTS, 5.0)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void testPoissonMean() {
        RandomService rs = new RandomService(77L);
        double sum = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            sum += rs.nextPoisson(RandomService.EVENTS, 5.0);
        }
        double mean = sum / n;
        assertThat(mean).isCloseTo(5.0, within(0.1));
    }

    @Test
    void testUnknownStreamThrows() {
        RandomService rs = new RandomService(1L);
        assertThatThrownBy(() -> rs.nextDouble("bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
