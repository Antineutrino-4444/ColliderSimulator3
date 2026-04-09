package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TwissParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LatticeTest {

    private static final double ENERGY_26GEV = 26.0;
    private static final double GAMMA_26 = ENERGY_26GEV / Bunch.PROTON_MASS;

    @Test
    void testFodoCellTune() {
        Lattice lat = new Lattice("FODO_test", 1000.0);
        double gradient = 1.0; // T/m – mild enough to remain stable at 26 GeV

        for (int i = 0; i < 10; i++) {
            lat.addElement(new Quadrupole("QF" + i, 0.5, gradient));
            lat.addElement(new Drift("D1_" + i, 5.0));
            lat.addElement(new Quadrupole("QD" + i, 0.5, -gradient));
            lat.addElement(new Drift("D2_" + i, 5.0));
        }

        double tuneX = lat.computeTuneX(GAMMA_26);
        double tuneY = lat.computeTuneY(GAMMA_26);

        assertThat(tuneX).isBetween(0.0, 10.0);
        assertThat(tuneY).isBetween(0.0, 10.0);
    }

    @Test
    void testTwissPropagation() {
        Lattice lat = new Lattice("twiss_test", 100.0);
        lat.addElement(new Drift("D1", 5.0));
        lat.addElement(new Quadrupole("QF", 2.0, 10.0));
        lat.addElement(new Drift("D2", 5.0));

        TwissParameters initial = new TwissParameters(10.0, 0.0, 10.0, 0.0, 0.0, 0.0);
        List<Lattice.TwissAtS> twissAlong = lat.computeTwissAlong(initial, GAMMA_26);

        assertThat(twissAlong.size()).isGreaterThan(1);
        // Beta should change through the lattice
        double betaFirst = twissAlong.get(0).twiss().getBetaX();
        double betaLast = twissAlong.get(twissAlong.size() - 1).twiss().getBetaX();
        assertThat(betaFirst).isNotCloseTo(betaLast, within(1e-6));
    }

    @Test
    void testSPositions() {
        Lattice lat = new Lattice("s_test", 100.0);
        lat.addElement(new Drift("D1", 10.0));
        lat.addElement(new Quadrupole("Q1", 2.0, 5.0));
        lat.addElement(new Drift("D2", 8.0));

        var elements = lat.getElements();
        assertThat(elements.get(0).getSPosition()).isCloseTo(0.0, within(1e-12));
        assertThat(elements.get(1).getSPosition()).isCloseTo(10.0, within(1e-12));
        assertThat(elements.get(2).getSPosition()).isCloseTo(12.0, within(1e-12));
    }
}
