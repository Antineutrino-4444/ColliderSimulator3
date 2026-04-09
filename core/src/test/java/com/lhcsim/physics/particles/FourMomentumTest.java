package com.lhcsim.physics.particles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FourMomentumTest {

    @Test
    void testAtRestMass() {
        FourMomentum p = FourMomentum.atRest(125.25);
        assertThat(p.invariantMass()).isCloseTo(125.25, within(1e-10));
    }

    @Test
    void testDiphotonHiggs() {
        // Two photons along +z and -z with E = mH/2
        double halfMass = 125.25 / 2.0;
        FourMomentum photon1 = new FourMomentum(halfMass, 0, 0, halfMass);
        FourMomentum photon2 = new FourMomentum(halfMass, 0, 0, -halfMass);
        FourMomentum sum = photon1.add(photon2);
        assertThat(sum.invariantMass()).isCloseTo(125.25, within(1e-10));
    }

    @Test
    void testPtCalculation() {
        FourMomentum p = new FourMomentum(100, 30, 40, 0);
        assertThat(p.pT()).isCloseTo(50.0, within(1e-10));
    }

    @Test
    void testBoostPreservesMass() {
        FourMomentum p = FourMomentum.atRest(125.25);
        FourMomentum boosted = p.boostZ(1.0);
        assertThat(boosted.invariantMass()).isCloseTo(125.25, within(1e-6));
    }

    @Test
    void testAddition() {
        FourMomentum a = new FourMomentum(10, 1, 2, 3);
        FourMomentum b = new FourMomentum(20, 4, 5, 6);
        FourMomentum sum = a.add(b);
        assertThat(sum.getE()).isCloseTo(30.0, within(1e-10));
        assertThat(sum.getPx()).isCloseTo(5.0, within(1e-10));
        assertThat(sum.getPy()).isCloseTo(7.0, within(1e-10));
        assertThat(sum.getPz()).isCloseTo(9.0, within(1e-10));
    }
}
