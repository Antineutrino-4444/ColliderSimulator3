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
     * Draw from a Poisson distribution with the given mean.
     */
    public int nextPoisson(String stream, double mean) {
        if (mean <= 0.0) {
            return 0;
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
