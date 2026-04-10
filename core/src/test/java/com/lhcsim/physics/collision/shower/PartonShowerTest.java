package com.lhcsim.physics.collision.shower;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.particles.FourMomentum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartonShowerTest {

    private PartonShower shower;

    @BeforeEach
    void setup() {
        shower = new PartonShower(new RngStream("test-shower", 12345L));
    }

    @Test
    void showerProducesMultiplePartons() {
        // A high-energy gluon should produce multiple partons
        FourMomentum p = new FourMomentum(500, 0, 0, 500);
        double q2 = 500 * 500;

        List<PartonShower.ShowerParton> partons = shower.shower(21, p, q2);
        assertThat(partons.size()).isGreaterThan(1);
    }

    @Test
    void multiplicityGrowsWithScale() {
        // Average multiplicity should increase with Q²
        double avgLow = averageMultiplicity(100);   // Q = 10 GeV
        double avgHigh = averageMultiplicity(10000); // Q = 100 GeV

        assertThat(avgHigh).isGreaterThan(avgLow);
    }

    @Test
    void showerRunsWithinTimeBudget() {
        // Should complete in < 0.5ms per event (test 100 events < 50ms)
        FourMomentum p = new FourMomentum(500, 0, 0, 500);
        double q2 = 500 * 500;

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            shower.shower(21, p, q2);
        }
        long elapsed = System.nanoTime() - start;
        double msPerEvent = elapsed / 1e6 / 100;

        assertThat(msPerEvent).isLessThan(0.5);
    }

    @Test
    void quarkShowerContainsGluons() {
        FourMomentum p = new FourMomentum(200, 0, 0, 200);
        List<PartonShower.ShowerParton> partons = shower.shower(2, p, 200 * 200);

        boolean hasGluon = partons.stream().anyMatch(sp -> sp.pdgId() == 21);
        // With high energy, q -> qg should produce at least one gluon most of the time
        // Run multiple trials
        int gluonCount = 0;
        for (int i = 0; i < 100; i++) {
            partons = shower.shower(2, p, 200 * 200);
            if (partons.stream().anyMatch(sp -> sp.pdgId() == 21)) {
                gluonCount++;
            }
        }
        assertThat(gluonCount).isGreaterThan(50);
    }

    @Test
    void lowScaleProducesNoEmissions() {
        // At Q^2 close to cutoff, should produce just the original parton
        FourMomentum p = new FourMomentum(1, 0, 0, 1);
        List<PartonShower.ShowerParton> partons = shower.shower(2, p, 1.5);

        assertThat(partons.size()).isEqualTo(1);
    }

    @Test
    void runningAlphaSDecreasesWithScale() {
        double asLow = PartonShower.alphaS(10);
        double asHigh = PartonShower.alphaS(10000);
        assertThat(asLow).isGreaterThan(asHigh);
    }

    private double averageMultiplicity(double q2) {
        double totalMult = 0;
        int trials = 200;
        double energy = Math.sqrt(q2);
        FourMomentum p = new FourMomentum(energy, 0, 0, energy);

        for (int i = 0; i < trials; i++) {
            totalMult += shower.shower(21, p, q2).size();
        }
        return totalMult / trials;
    }
}
