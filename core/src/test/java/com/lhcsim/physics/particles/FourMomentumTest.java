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

    // ── Milestone 3 tests: LorentzBoost + extended FourMomentum ─────

    @Test
    void generalBoostPreservesMassToHighPrecision() {
        FourMomentum p = FourMomentum.atRest(125.25);
        LorentzBoost boost = new LorentzBoost(0.3, 0.2, 0.5);
        FourMomentum boosted = p.boost(boost);
        assertThat(boosted.invariantMass()).isCloseTo(125.25, within(1e-10));
    }

    @Test
    void boostAndInverseBoostIsIdentity() {
        FourMomentum p = new FourMomentum(100, 30, 40, 50);
        LorentzBoost boost = new LorentzBoost(0.2, -0.1, 0.3);
        FourMomentum boosted = p.boost(boost);
        FourMomentum restored = boosted.boost(boost.inverse());

        assertThat(restored.getE()).isCloseTo(p.getE(), within(1e-10));
        assertThat(restored.getPx()).isCloseTo(p.getPx(), within(1e-10));
        assertThat(restored.getPy()).isCloseTo(p.getPy(), within(1e-10));
        assertThat(restored.getPz()).isCloseTo(p.getPz(), within(1e-10));
    }

    @Test
    void boostFromFourMomentumBringsToRest() {
        double mass = 91.1876;
        FourMomentum p = FourMomentum.fromMassAndMomentum(mass, 50, 30, 80);
        // fromFourMomentum(p) creates β = p/E; applying it directly brings to rest
        LorentzBoost toRest = LorentzBoost.fromFourMomentum(p);
        FourMomentum atRest = toRest.apply(p);

        assertThat(atRest.getPx()).isCloseTo(0, within(1e-8));
        assertThat(atRest.getPy()).isCloseTo(0, within(1e-8));
        assertThat(atRest.getPz()).isCloseTo(0, within(1e-8));
        assertThat(atRest.getE()).isCloseTo(mass, within(1e-8));
    }

    @Test
    void backToBackDecayInvariantUnderBoost() {
        // Two equal-mass particles back-to-back in a boosted frame
        // should have the same invariant mass as in the rest frame
        double m = 0.13957; // pion mass
        double pStar = 45.0;
        FourMomentum p1 = FourMomentum.fromMassAndMomentum(m, pStar, 0, 0);
        FourMomentum p2 = FourMomentum.fromMassAndMomentum(m, -pStar, 0, 0);
        double restMass = p1.add(p2).invariantMass();

        LorentzBoost boost = new LorentzBoost(0.0, 0.0, 0.5);
        FourMomentum p1b = boost.apply(p1);
        FourMomentum p2b = boost.apply(p2);
        double boostedMass = p1b.add(p2b).invariantMass();

        assertThat(boostedMass).isCloseTo(restMass, within(1e-10));
    }

    @Test
    void subtractAndScale() {
        FourMomentum a = new FourMomentum(10, 1, 2, 3);
        FourMomentum b = new FourMomentum(4, 1, 1, 1);
        FourMomentum diff = a.subtract(b);
        assertThat(diff.getE()).isCloseTo(6.0, within(1e-10));

        FourMomentum scaled = a.scale(2.0);
        assertThat(scaled.getE()).isCloseTo(20.0, within(1e-10));
        assertThat(scaled.getPx()).isCloseTo(2.0, within(1e-10));
    }

    @Test
    void mass2Method() {
        FourMomentum p = FourMomentum.atRest(91.1876);
        assertThat(p.mass2()).isCloseTo(91.1876 * 91.1876, within(1e-6));
    }
}
