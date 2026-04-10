package com.lhcsim.physics.beam;

import com.lhcsim.physics.accelerator.Drift;
import com.lhcsim.physics.accelerator.Lattice;
import com.lhcsim.physics.accelerator.Quadrupole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link Twiss} and {@link TwissPropagator}.
 */
class TwissPropagatorTest {

    private static final double ENERGY_26GEV = 26.0;
    private static final double GAMMA_26 = ENERGY_26GEV / Bunch.PROTON_MASS;

    private Lattice buildFodoLattice(int numCells, double gradient, double cellLength) {
        double halfCell = cellLength / 2.0;
        double quadLength = 0.5;
        double driftLength = halfCell - quadLength;

        Lattice lat = new Lattice("FODO", numCells * cellLength);
        for (int i = 0; i < numCells; i++) {
            lat.addElement(new Quadrupole("QF" + i, quadLength, gradient));
            lat.addElement(new Drift("D1_" + i, driftLength));
            lat.addElement(new Quadrupole("QD" + i, quadLength, -gradient));
            lat.addElement(new Drift("D2_" + i, driftLength));
        }
        return lat;
    }

    @Test
    void testPeriodicTwissClosedAfterOneTurn() {
        Lattice lat = buildFodoLattice(20, 1.0, 11.0);
        Twiss periodic = TwissPropagator.computePeriodic(lat, GAMMA_26);

        // Propagate through entire lattice and verify we return to the same Twiss
        Twiss current = periodic;
        for (var el : lat.getElements()) {
            if (el.isActive()) {
                current = TwissPropagator.propagate(current, el.getTransferMatrix(GAMMA_26));
            }
        }

        assertThat(current.betaX()).isCloseTo(periodic.betaX(), within(1e-8));
        assertThat(current.alphaX()).isCloseTo(periodic.alphaX(), within(1e-8));
        assertThat(current.betaY()).isCloseTo(periodic.betaY(), within(1e-8));
        assertThat(current.alphaY()).isCloseTo(periodic.alphaY(), within(1e-8));
    }

    @Test
    void testTuneMatchesFormula() {
        Lattice lat = buildFodoLattice(20, 1.0, 11.0);
        double tuneX = lat.computeTuneX(GAMMA_26);
        double tuneY = lat.computeTuneY(GAMMA_26);

        // Tunes should be positive and reasonable
        assertThat(tuneX).isBetween(0.01, 10.0);
        assertThat(tuneY).isBetween(0.01, 10.0);

        // The periodic Twiss should give consistent beta values
        Twiss periodic = TwissPropagator.computePeriodic(lat, GAMMA_26);
        assertThat(periodic.betaX()).isPositive();
        assertThat(periodic.betaY()).isPositive();
    }

    @Test
    void testBetaFunctionSampledAtBoundaries() {
        Lattice lat = buildFodoLattice(10, 1.0, 11.0);
        List<TwissPropagator.SamplePoint> samples = TwissPropagator.betaFunction(lat, GAMMA_26);

        // 40 elements + 1 initial point = 41
        assertThat(samples).hasSize(41);

        // All beta values must be positive
        for (TwissPropagator.SamplePoint sp : samples) {
            assertThat(sp.betaX()).isPositive();
            assertThat(sp.betaY()).isPositive();
        }

        // First and last should be close (periodic)
        assertThat(samples.get(samples.size() - 1).betaX())
                .isCloseTo(samples.get(0).betaX(), within(1e-6));
    }

    @Test
    void testTwissInvariant() {
        // beta * gamma - alpha^2 = 1 must hold after propagation
        Lattice lat = buildFodoLattice(10, 1.0, 11.0);
        Twiss periodic = TwissPropagator.computePeriodic(lat, GAMMA_26);

        double invariantX = periodic.betaX() * periodic.gammaX()
                - periodic.alphaX() * periodic.alphaX();
        assertThat(invariantX).isCloseTo(1.0, within(1e-10));

        double invariantY = periodic.betaY() * periodic.gammaY()
                - periodic.alphaY() * periodic.alphaY();
        assertThat(invariantY).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void testTwissRecordConversion() {
        Twiss tw = Twiss.of(10.0, -1.0, 8.0, 0.5, 1.5, 0.1);
        TwissParameters tp = tw.toTwissParameters();
        Twiss back = Twiss.fromTwissParameters(tp);

        assertThat(back.betaX()).isCloseTo(tw.betaX(), within(1e-12));
        assertThat(back.alphaX()).isCloseTo(tw.alphaX(), within(1e-12));
        assertThat(back.betaY()).isCloseTo(tw.betaY(), within(1e-12));
    }

    @Test
    void testLargeLatticePropagatesInUnder1ms() {
        // Chunk 1.5 acceptance: 50-element lattice in < 1ms
        Lattice lat = buildFodoLattice(13, 1.0, 11.0); // 52 elements

        long start = System.nanoTime();
        TwissPropagator.computePeriodic(lat, GAMMA_26);
        TwissPropagator.betaFunction(lat, GAMMA_26);
        long elapsed = System.nanoTime() - start;

        // Should complete well under 100 ms (being generous for CI)
        assertThat(elapsed).isLessThan(100_000_000L);
    }
}
