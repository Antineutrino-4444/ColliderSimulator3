package com.lhcsim.physics.collision;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.beam.Beam;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TwissParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EventSamplerTest {

    private static CrossSectionDatabase crossSectionDb;
    private static final double ECM_TEV = 13.6;

    @BeforeAll
    static void setup() throws IOException {
        crossSectionDb = CrossSectionDatabase.loadDefault();
    }

    private Beam lhcBeam() {
        TwissParameters tw = new TwissParameters(0.55, 0, 0.55, 0, 0, 0);
        Bunch b = new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.0755, 1.13e-4, 6800.0, tw);
        return Beam.uniform(b, 2808, 3564, 26_658.883);
    }

    @Test
    void testMeanEventCountMatchesAnalytic() {
        RandomService random = new RandomService(42L);
        EventSampler sampler = new EventSampler(crossSectionDb, random);

        Beam b1 = lhcBeam();
        Beam b2 = lhcBeam();
        InteractionPoint ip = InteractionPoint.lhcNominal("IP1");

        // Get the instantaneous luminosity for this configuration
        double lumiCm2s = LuminosityCalculator.instantaneous(b1, b2, ip);

        // For a fixed luminosity, sample many ticks and check mean Higgs count
        double dtSeconds = 1.0; // 1 second
        double sigmaHiggs = crossSectionDb.get(Process.HIGGS_GGF, ECM_TEV);
        double intLumiPb = lumiCm2s * dtSeconds * 1e-36;
        double expectedHiggs = sigmaHiggs * intLumiPb;

        int numTrials = 10_000;
        long totalHiggs = 0;
        for (int i = 0; i < numTrials; i++) {
            Map<Process, Integer> counts = sampler.sampleFromLuminosity(
                    lumiCm2s, dtSeconds, ECM_TEV);
            totalHiggs += counts.getOrDefault(Process.HIGGS_GGF, 0);
        }

        double meanHiggs = (double) totalHiggs / numTrials;
        // Mean should be within 5% of analytic (statistical tolerance for 10k trials)
        assertThat(meanHiggs).isCloseTo(expectedHiggs, within(expectedHiggs * 0.05));
    }

    @Test
    void testVarianceMatchesPoisson() {
        RandomService random = new RandomService(123L);
        EventSampler sampler = new EventSampler(crossSectionDb, random);

        // Use a fixed luminosity that gives a moderate Higgs rate
        double lumiCm2s = 1.0e34; // cm^-2 s^-1
        double dtSeconds = 1.0;
        double sigmaHiggs = crossSectionDb.get(Process.HIGGS_GGF, ECM_TEV);
        double intLumiPb = lumiCm2s * dtSeconds * 1e-36;
        double expectedHiggs = sigmaHiggs * intLumiPb;

        int numTrials = 10_000;
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < numTrials; i++) {
            Map<Process, Integer> counts = sampler.sampleFromLuminosity(
                    lumiCm2s, dtSeconds, ECM_TEV);
            int n = counts.getOrDefault(Process.HIGGS_GGF, 0);
            sum += n;
            sumSq += (double) n * n;
        }

        double mean = sum / numTrials;
        double variance = sumSq / numTrials - mean * mean;

        // For Poisson, variance = mean
        // Allow 20% tolerance for statistical fluctuation
        assertThat(variance).isCloseTo(mean, within(mean * 0.20 + 0.5));
    }

    @Test
    void testNoInelasticInSamples() {
        RandomService random = new RandomService(999L);
        EventSampler sampler = new EventSampler(crossSectionDb, random);

        double lumiCm2s = 1.0e34;
        Map<Process, Integer> counts = sampler.sampleFromLuminosity(
                lumiCm2s, 1.0, ECM_TEV);

        // Inelastic should never appear in the samples
        assertThat(counts.containsKey(Process.INELASTIC_PP)).isFalse();
    }
}
