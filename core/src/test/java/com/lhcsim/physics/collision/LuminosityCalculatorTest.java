package com.lhcsim.physics.collision;

import com.lhcsim.physics.beam.Beam;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TwissParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LuminosityCalculatorTest {

    private Bunch lhcBunch(double energy, double numParticles) {
        TwissParameters tw = new TwissParameters(0.30, 0, 0.30, 0, 0, 0);
        return new Bunch(numParticles, 3.75e-6, 3.75e-6, 0.075, 1.13e-4, energy, tw);
    }

    @Test
    void testNominalLuminosity() {
        Bunch b1 = lhcBunch(6500.0, 1.15e11);
        Bunch b2 = lhcBunch(6500.0, 1.15e11);
        double lumi = LuminosityCalculator.instantaneousLuminosity(
                b1, b2, 2544, 285e-6, 26658.883, 0.30, 0.30);
        assertThat(lumi).isBetween(1e33, 1e35);
    }

    @Test
    void testScalesAsNSquared() {
        Bunch b1 = lhcBunch(6500.0, 1e11);
        Bunch b2 = lhcBunch(6500.0, 1e11);
        double lumi1 = LuminosityCalculator.instantaneousLuminosity(
                b1, b2, 2544, 285e-6, 26658.883, 0.30, 0.30);

        Bunch b3 = lhcBunch(6500.0, 2e11);
        Bunch b4 = lhcBunch(6500.0, 2e11);
        double lumi2 = LuminosityCalculator.instantaneousLuminosity(
                b3, b4, 2544, 285e-6, 26658.883, 0.30, 0.30);

        assertThat(lumi2 / lumi1).isCloseTo(4.0, within(0.01));
    }

    @Test
    void testCrossingAngleReduces() {
        Bunch b1 = lhcBunch(6500.0, 1.15e11);
        Bunch b2 = lhcBunch(6500.0, 1.15e11);

        double lumiSmall = LuminosityCalculator.instantaneousLuminosity(
                b1, b2, 2544, 100e-6, 26658.883, 0.30, 0.30);
        double lumiLarge = LuminosityCalculator.instantaneousLuminosity(
                b1, b2, 2544, 500e-6, 26658.883, 0.30, 0.30);

        assertThat(lumiLarge).isLessThan(lumiSmall);
    }

    @Test
    void testPileup() {
        Bunch b1 = lhcBunch(6500.0, 1.15e11);
        Bunch b2 = lhcBunch(6500.0, 1.15e11);
        double lumi = LuminosityCalculator.instantaneousLuminosity(
                b1, b2, 2544, 285e-6, 26658.883, 0.30, 0.30);

        double beta = b1.lorentzBeta();
        double fRev = 299_792_458.0 * beta / 26658.883;
        // Inelastic cross-section ~80 mb = 80e-27 cm^2
        double mu = LuminosityCalculator.meanPileup(lumi, 80e-27, fRev, 2544);
        assertThat(mu).isBetween(10.0, 100.0);
    }

    @Test
    void testUnitConversion() {
        double cm2 = 1e39;
        double fb = LuminosityCalculator.toInverseFemtobarns(cm2);
        double backCm2 = LuminosityCalculator.fromInverseFemtobarns(fb);
        assertThat(backCm2).isCloseTo(cm2, within(cm2 * 1e-10));
    }

    // ── Tests for the new Beam-based API ────────────────────────────

    @Test
    void testInstantaneousWithBeamApi() {
        TwissParameters tw = new TwissParameters(0.55, 0, 0.55, 0, 0, 0);
        Bunch bunch = new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.0755, 1.13e-4, 7000.0, tw);
        Beam b1 = Beam.uniform(bunch, 2808, 3564, 26_658.883);
        Beam b2 = Beam.uniform(bunch, 2808, 3564, 26_658.883);
        InteractionPoint ip = new InteractionPoint("IP1", 285e-6, 0.55, 0.55);

        double lumi = LuminosityCalculator.instantaneous(b1, b2, ip);
        // LHC design luminosity ~ 1e34 cm^-2 s^-1, within an order of magnitude
        assertThat(lumi).isBetween(1e33, 2e34);
    }
}
