package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.Twiss;
import com.lhcsim.physics.beam.TwissPropagator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link LatticeLoader} loading the simplified PS lattice.
 */
class LatticeLoaderTest {

    private static Lattice ps;
    private static final double PS_ENERGY = 26.0; // GeV extraction energy
    private static final double GAMMA_PS = PS_ENERGY / Bunch.PROTON_MASS;

    @BeforeAll
    static void loadPS() throws IOException {
        ps = LatticeLoader.loadResource("/data/lattices/ps.json");
    }

    @Test
    void testElementCount() {
        // 100 cells x 4 elements = 400
        assertThat(ps.getElements().size()).isEqualTo(400);
    }

    @Test
    void testCircumference() {
        assertThat(ps.totalLength()).isCloseTo(628.3, within(0.1));
    }

    @Test
    void testOneTurnMatrixIsStable() {
        var m = ps.computeOneTurnMatrix(GAMMA_PS);
        double traceX = m.get(0, 0) + m.get(1, 1);
        double traceY = m.get(2, 2) + m.get(3, 3);

        assertThat(Math.abs(traceX)).isLessThan(2.0);
        assertThat(Math.abs(traceY)).isLessThan(2.0);
    }

    @Test
    void testTuneX() {
        double tuneX = ps.computeTuneX(GAMMA_PS);
        // computeTuneX returns the fractional tune (phase advance mod 2pi / 2pi)
        // Target: Qx ~ 6.24, so fractional part ~ 0.24
        assertThat(tuneX).isCloseTo(0.24, within(0.05));
    }

    @Test
    void testTuneY() {
        double tuneY = ps.computeTuneY(GAMMA_PS);
        // Target: Qy ~ 6.27, so fractional part ~ 0.27
        assertThat(tuneY).isCloseTo(0.27, within(0.15));
    }

    @Test
    void testTwissCloses() {
        // The periodic Twiss should exist (no exception) and have positive beta
        Twiss periodic = TwissPropagator.computePeriodic(ps, GAMMA_PS);
        assertThat(periodic.betaX()).isPositive();
        assertThat(periodic.betaY()).isPositive();
    }

    @Test
    void testFindByName() {
        assertThat(ps.findByName("QF.000")).isNotNull();
        assertThat(ps.findByName("QD.050")).isNotNull();
        assertThat(ps.findByName("MB.099.2")).isNotNull();
    }
}
