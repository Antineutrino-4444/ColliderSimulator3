package com.lhcsim.physics.collision;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.beam.Beam;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes pileup (mean interactions per bunch crossing) and groups
 * events into bunch crossings.
 * <p>
 * Mean pileup: mu = L * sigma_inel / (f_rev * n_b)
 * <p>
 * Reference: LHC Design Report Vol. 1, Chapter 2.
 */
public class PileupCalculator {

    private final CrossSectionDatabase crossSectionDb;
    private final RandomService random;
    private long crossingCounter;

    public PileupCalculator(CrossSectionDatabase crossSectionDb, RandomService random) {
        this.crossSectionDb = crossSectionDb;
        this.random = random;
    }

    /**
     * Computes the mean number of inelastic interactions per bunch crossing.
     *
     * @param luminosity instantaneous luminosity [cm^-2 s^-1]
     * @param sigmaInel  inelastic pp cross-section [cm^2]
     * @param fRev       revolution frequency [Hz]
     * @param numBunches number of colliding bunch pairs
     * @return mean pileup mu
     */
    public static double mu(double luminosity, double sigmaInel,
                            double fRev, int numBunches) {
        if (fRev <= 0 || numBunches <= 0) return 0;
        return luminosity * sigmaInel / (fRev * numBunches);
    }

    /**
     * Generates bunch crossings for a time slice.
     * <p>
     * The number of crossings in the slice is f_rev * n_b * dt.
     * Each crossing gets:
     * - Poisson(mu) inelastic interactions
     * - Bernoulli draws per hard process with P = sigma_hard / sigma_inel * mu
     *
     * @param b1        beam 1
     * @param b2        beam 2
     * @param ip        interaction point
     * @param dtSeconds time slice [s]
     * @param ecmTeV    centre-of-mass energy [TeV]
     * @param timestamp current simulation time [s]
     * @return list of bunch crossings (may be capped for performance)
     */
    public List<BunchCrossing> generateCrossings(Beam b1, Beam b2, InteractionPoint ip,
                                                  double dtSeconds, double ecmTeV,
                                                  double timestamp) {
        double lumiCm2s = LuminosityCalculator.instantaneous(b1, b2, ip);
        double fRev = b1.revolutionFrequency();
        int numBunches = Math.min(b1.getFilledBuckets(), b2.getFilledBuckets());

        // Inelastic cross-section in cm^2
        double sigmaInelPb = crossSectionDb.get(Process.INELASTIC_PP, ecmTeV);
        double sigmaInelCm2 = sigmaInelPb * 1e-36; // 1 pb = 1e-36 cm^2

        double meanMu = mu(lumiCm2s, sigmaInelCm2, fRev, numBunches);

        // Number of crossings in this time slice
        long totalCrossings = (long) (fRev * numBunches * dtSeconds);

        // Cap for performance: don't simulate millions of crossings per tick
        int maxCrossings = 1000;
        int nCrossings = (int) Math.min(totalCrossings, maxCrossings);

        // Hard process probabilities: P(hard in crossing) = sigma_hard * L_per_crossing
        // where L_per_crossing = L / (f_rev * n_b)
        double lumiPerCrossing = lumiCm2s / (fRev * numBunches);

        List<BunchCrossing> crossings = new ArrayList<>(nCrossings);
        for (int i = 0; i < nCrossings; i++) {
            int nInelastic = random.nextPoisson(RandomService.EVENTS, meanMu);

            List<Process> hardProcs = new ArrayList<>();
            for (Process proc : Process.values()) {
                if (proc == Process.INELASTIC_PP) continue;

                double sigmaPb;
                try {
                    sigmaPb = crossSectionDb.get(proc, ecmTeV);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (sigmaPb <= 0) continue;

                double sigmaCm2 = sigmaPb * 1e-36;
                double pHard = lumiPerCrossing * sigmaCm2;
                if (random.nextDouble(RandomService.EVENTS) < pHard) {
                    hardProcs.add(proc);
                }
            }

            double bxTime = timestamp + (i * dtSeconds / nCrossings);
            crossings.add(new BunchCrossing(crossingCounter++, bxTime, nInelastic, hardProcs));
        }
        return crossings;
    }

    /**
     * Returns the mean pileup for the current beam/IP configuration.
     */
    public double computeMu(Beam b1, Beam b2, InteractionPoint ip, double ecmTeV) {
        double lumiCm2s = LuminosityCalculator.instantaneous(b1, b2, ip);
        double fRev = b1.revolutionFrequency();
        int numBunches = Math.min(b1.getFilledBuckets(), b2.getFilledBuckets());

        double sigmaInelPb = crossSectionDb.get(Process.INELASTIC_PP, ecmTeV);
        double sigmaInelCm2 = sigmaInelPb * 1e-36;

        return mu(lumiCm2s, sigmaInelCm2, fRev, numBunches);
    }
}
