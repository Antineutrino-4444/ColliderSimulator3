package com.lhcsim.physics.collision;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.beam.Beam;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-tick Poisson event sampler.
 * <p>
 * For each physics process, computes the expected number of events
 * from L * sigma * dt, then draws from a Poisson distribution.
 */
public class EventSampler {

    private final CrossSectionDatabase crossSectionDb;
    private final RandomService random;

    public EventSampler(CrossSectionDatabase crossSectionDb, RandomService random) {
        this.crossSectionDb = crossSectionDb;
        this.random = random;
    }

    /**
     * Samples event counts for each process during a time slice.
     * <p>
     * expected = sigma(process, ecm) * L_inst * dt
     * where sigma is in pb, L_inst is in cm^-2 s^-1, and dt is in seconds.
     *
     * @param b1        beam 1
     * @param b2        beam 2
     * @param ip        interaction point
     * @param dtSeconds time interval [s]
     * @param ecmTeV    centre-of-mass energy [TeV]
     * @return map from Process to sampled event count
     */
    public Map<Process, Integer> sample(Beam b1, Beam b2, InteractionPoint ip,
                                         double dtSeconds, double ecmTeV) {
        double lumiCm2s = LuminosityCalculator.instantaneous(b1, b2, ip);
        return sampleFromLuminosity(lumiCm2s, dtSeconds, ecmTeV);
    }

    /**
     * Samples event counts given a pre-computed luminosity.
     *
     * @param lumiCm2s instantaneous luminosity [cm^-2 s^-1]
     * @param dtSeconds time interval [s]
     * @param ecmTeV    centre-of-mass energy [TeV]
     * @return map from Process to sampled event count
     */
    public Map<Process, Integer> sampleFromLuminosity(double lumiCm2s,
                                                       double dtSeconds,
                                                       double ecmTeV) {
        EnumMap<Process, Integer> counts = new EnumMap<>(Process.class);

        // Convert L_inst * dt from cm^-2 to pb^-1
        // 1 pb = 1e-36 cm^2, so 1 pb^-1 = 1e36 cm^-2
        double intLumiPb = lumiCm2s * dtSeconds * 1e-36;

        for (Process proc : Process.values()) {
            // Skip total_inelastic: it's for pileup calculation only
            if (proc == Process.INELASTIC_PP) continue;

            double sigma;
            try {
                sigma = crossSectionDb.get(proc, ecmTeV);
            } catch (IllegalArgumentException e) {
                // Process not available at this energy — skip
                continue;
            }
            if (sigma <= 0) continue;

            double expected = sigma * intLumiPb;
            int drawn = random.nextPoisson(RandomService.EVENTS, expected);
            if (drawn > 0) {
                counts.put(proc, drawn);
            }
        }
        return counts;
    }
}
