package com.lhcsim.physics.beam;

import com.lhcsim.physics.Constants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BeamTest {

    /**
     * LHC nominal bunch at 7000 GeV with beta* = 0.55 m.
     */
    private Bunch lhcNominalBunch() {
        TwissParameters tw = new TwissParameters(0.55, 0, 0.55, 0, 0, 0);
        return new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.0755, 1.13e-4, 7000.0, tw);
    }

    @Test
    void testSigmaXAtIp() {
        Bunch b = lhcNominalBunch();
        // sigma_x = sqrt(emittanceN / (beta*gamma) * beta*)
        // gamma ~ 7461, beta ~ 1, geomEmit ~ 3.75e-6 / 7461 ~ 5.03e-10
        // sigma ~ sqrt(5.03e-10 * 0.55) ~ 1.66e-5 m = 16.6 um
        double sigmaX = b.sigmaXAtIp();
        assertThat(sigmaX * 1e6).isCloseTo(16.7, within(1.0)); // 16.7 um
    }

    @Test
    void testSigmaYAtIp() {
        Bunch b = lhcNominalBunch();
        double sigmaY = b.sigmaYAtIp();
        assertThat(sigmaY * 1e6).isCloseTo(16.7, within(1.0)); // 16.7 um
    }

    @Test
    void testBeamRevolutionFrequency() {
        Bunch template = lhcNominalBunch();
        Beam beam = Beam.uniform(template, 2808, 3564, 26_658.883);
        double fRev = beam.revolutionFrequency();
        // f_rev ~ c / C ~ 299792458 / 26658.883 ~ 11245 Hz
        assertThat(fRev).isCloseTo(11245.0, within(5.0));
    }

    @Test
    void testBeamFilledBuckets() {
        Bunch template = lhcNominalBunch();
        Beam beam = Beam.uniform(template, 2808, 3564, 26_658.883);
        assertThat(beam.getFilledBuckets()).isEqualTo(2808);
        assertThat(beam.getTotalBuckets()).isEqualTo(3564);
    }

    @Test
    void testBeamEnergyFromBunches() {
        Bunch template = lhcNominalBunch();
        Beam beam = Beam.uniform(template, 2808, 3564, 26_658.883);
        assertThat(beam.energyGeV()).isCloseTo(7000.0, within(0.1));
    }

    @Test
    void testBeamMeanProtonsPerBunch() {
        Bunch template = lhcNominalBunch();
        Beam beam = Beam.uniform(template, 2808, 3564, 26_658.883);
        assertThat(beam.meanProtonsPerBunch()).isCloseTo(1.15e11, within(1e8));
    }
}
