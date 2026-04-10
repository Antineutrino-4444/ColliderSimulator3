package com.lhcsim.physics.beam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BunchTest {

    private Bunch nominalBunch() {
        TwissParameters tw = new TwissParameters(0.55, 0, 0.55, 0, 0, 0);
        return new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.075, 1.13e-4, 7000.0, tw);
    }

    @Test
    void testLorentzGamma() {
        Bunch b = nominalBunch();
        assertThat(b.lorentzGamma()).isCloseTo(7461.0, within(1.0));
    }

    @Test
    void testLorentzBeta() {
        Bunch b = nominalBunch();
        assertThat(b.lorentzBeta()).isCloseTo(1.0, within(1e-8));
    }

    @Test
    void testMagneticRigidity() {
        Bunch b = nominalBunch();
        assertThat(b.magneticRigidity()).isCloseTo(23349.0, within(10.0));
    }

    @Test
    void testEmittanceDamping() {
        TwissParameters tw = new TwissParameters(1.0, 0, 1.0, 0, 0, 0);
        Bunch low = new Bunch(1e11, 3.75e-6, 3.75e-6, 0.075, 1e-4, 450.0, tw);
        Bunch high = new Bunch(1e11, 3.75e-6, 3.75e-6, 0.075, 1e-4, 7000.0, tw);

        assertThat(high.geometricEmittanceX()).isLessThan(low.geometricEmittanceX());
    }
}
