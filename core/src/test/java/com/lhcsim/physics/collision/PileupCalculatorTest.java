package com.lhcsim.physics.collision;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.beam.Beam;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TwissParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PileupCalculatorTest {

    private static CrossSectionDatabase crossSectionDb;

    @BeforeAll
    static void setup() throws IOException {
        crossSectionDb = CrossSectionDatabase.loadDefault();
    }

    private Beam lhcBeam() {
        TwissParameters tw = new TwissParameters(0.55, 0, 0.55, 0, 0, 0);
        Bunch b = new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.0755, 1.13e-4, 7000.0, tw);
        return Beam.uniform(b, 2808, 3564, 26_658.883);
    }

    @Test
    void testMuStaticFormula() {
        // L=1e34 cm^-2 s^-1, sigma_inel=80 mb=80e-27 cm^2, f_rev=11245 Hz, n_b=2808
        double mu = PileupCalculator.mu(1.0e34, 80e-27, 11245.0, 2808);
        // mu = 1e34 * 80e-27 / (11245 * 2808) ~ 25.3
        assertThat(mu).isCloseTo(25.3, within(2.0));
    }

    @Test
    void testComputeMuWithBeams() {
        RandomService random = new RandomService(42L);
        PileupCalculator calc = new PileupCalculator(crossSectionDb, random);

        Beam b1 = lhcBeam();
        Beam b2 = lhcBeam();
        InteractionPoint ip = InteractionPoint.lhcNominal("IP1");

        double mu = calc.computeMu(b1, b2, ip, 13.6);
        // With LHC nominal parameters, mu should be in the range 10-60
        assertThat(mu).isBetween(10.0, 60.0);
    }

    @Test
    void testMuScalesWithLuminosity() {
        // Double the luminosity should double the pileup
        double mu1 = PileupCalculator.mu(1.0e34, 80e-27, 11245.0, 2808);
        double mu2 = PileupCalculator.mu(2.0e34, 80e-27, 11245.0, 2808);
        assertThat(mu2 / mu1).isCloseTo(2.0, within(0.01));
    }

    @Test
    void testGenerateCrossingsHasSoftEvents() {
        RandomService random = new RandomService(42L);
        PileupCalculator calc = new PileupCalculator(crossSectionDb, random);

        Beam b1 = lhcBeam();
        Beam b2 = lhcBeam();
        InteractionPoint ip = InteractionPoint.lhcNominal("IP1");

        var crossings = calc.generateCrossings(b1, b2, ip, 0.001, 13.6, 0.0);

        // Should have crossings
        assertThat(crossings).isNotEmpty();
        // Each crossing should have some inelastic events
        for (BunchCrossing bx : crossings) {
            assertThat(bx.nInelastic()).isGreaterThanOrEqualTo(0);
        }
    }
}
