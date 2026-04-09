package com.lhcsim.core;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized seeded random-number service. Each named stream gets a
 * deterministic seed derived from the master seed so simulation runs
 * are fully reproducible.
 */
public final class RandomService {

    public static final String OPTICS   = "OPTICS";
    public static final String EVENTS   = "EVENTS";
    public static final String ALERTS   = "ALERTS";
    public static final String DETECTOR = "DETECTOR";
    public static final String BSM      = "BSM";

    private static final String[] DEFAULT_STREAMS = {
            OPTICS, EVENTS, ALERTS, DETECTOR, BSM
    };

    private long masterSeed;
    private final Map<String, MersenneTwister> streams = new LinkedHashMap<>();

    public RandomService(long masterSeed) {
        this.masterSeed = masterSeed;
        initStreams();
    }

    private void initStreams() {
        streams.clear();
        for (int i = 0; i < DEFAULT_STREAMS.length; i++) {
            long derived = masterSeed ^ (0x9E3779B97F4A7C15L * (i + 1));
            streams.put(DEFAULT_STREAMS[i], new MersenneTwister(derived));
        }
    }

    /** Reset all streams from a new master seed. */
    public void reseed(long newMasterSeed) {
        this.masterSeed = newMasterSeed;
        initStreams();
    }

    public long getMasterSeed() {
        return masterSeed;
    }

    public MersenneTwister getStream(String name) {
        MersenneTwister mt = streams.get(name);
        if (mt == null) {
            throw new IllegalArgumentException("Unknown RNG stream: " + name);
        }
        return mt;
    }

    public double nextGaussian(String stream, double mean, double sigma) {
        return mean + getStream(stream).nextGaussian() * sigma;
    }

    public double nextDouble(String stream) {
        return getStream(stream).nextDouble();
    }

    public int nextInt(String stream, int bound) {
        return getStream(stream).nextInt(bound);
    }

    /**
     * Threshold above which we switch from the exact Poisson sampler
     * (Apache Commons Math) to a fast Gaussian approximation.
     * For large means the CLT gives Poisson(λ) ≈ N(λ, λ), and the
     * exact sampler becomes very slow (~4 ms per call for λ > 100 000).
     */
    private static final double POISSON_NORMAL_THRESHOLD = 1000.0;

    /**
     * Draw from a Poisson distribution with the given mean.
     * <p>
     * For large means (λ > {@value #POISSON_NORMAL_THRESHOLD}) a Gaussian
     * approximation {@code round(λ + √λ · z)} is used instead of the
     * exact rejection sampler, which is prohibitively slow for the event
     * rates typical at the LHC (W production alone: ~200 000 pb × L).
     */
    public int nextPoisson(String stream, double mean) {
        if (mean <= 0.0) {
            return 0;
        }
        if (mean >= POISSON_NORMAL_THRESHOLD) {
            // Gaussian approximation: Poisson(λ) ≈ N(λ, λ)
            double sample = mean + Math.sqrt(mean) * getStream(stream).nextGaussian();
            return Math.max(0, (int) Math.round(sample));
        }
        PoissonDistribution poisson = new PoissonDistribution(
                getStream(stream), mean,
                PoissonDistribution.DEFAULT_EPSILON,
                PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        return poisson.sample();
    }

    /**
     * Draw from an exponential distribution with the given mean,
     * guarding against log(0).
     */
    public double nextExponential(String stream, double mean) {
        double u;
        do {
            u = nextDouble(stream);
        } while (u == 0.0);
        return -mean * Math.log(u);
    }
}
