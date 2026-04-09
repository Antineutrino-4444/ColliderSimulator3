package com.lhcsim.core;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.Well19937c;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized seeded random-number service.  Each named stream gets a
 * deterministic seed derived from the master seed so simulation runs
 * are fully reproducible.
 * <p>
 * Underlying generator: {@link Well19937c} (see Chunk 0.5).
 * Seed derivation: {@code masterSeed XOR hash(streamName)}.
 * <p>
 * Streams: {@code optics}, {@code events}, {@code alerts}, {@code detector},
 * {@code bsm}, {@code ui}.
 */
public final class RandomService {

    public static final String OPTICS   = "optics";
    public static final String EVENTS   = "events";
    public static final String ALERTS   = "alerts";
    public static final String DETECTOR = "detector";
    public static final String BSM      = "bsm";
    public static final String UI       = "ui";

    private static final String[] DEFAULT_STREAMS = {
            OPTICS, EVENTS, ALERTS, DETECTOR, BSM, UI
    };

    private long masterSeed;
    private final Map<String, RngStream> streams = new LinkedHashMap<>();

    // Backward compat: expose Well19937c as MersenneTwister was before
    // (callers that accessed getStream(String) directly)
    private final Map<String, Well19937c> rawStreams = new LinkedHashMap<>();

    public RandomService(long masterSeed) {
        this.masterSeed = masterSeed;
        initStreams();
    }

    private void initStreams() {
        streams.clear();
        rawStreams.clear();
        for (String name : DEFAULT_STREAMS) {
            long derived = masterSeed ^ (long) name.hashCode();
            RngStream rs = new RngStream(name, derived);
            streams.put(name, rs);
            rawStreams.put(name, rs.getRng());
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

    /** Get the named {@link RngStream} wrapper. */
    public RngStream stream(String name) {
        RngStream rs = streams.get(name);
        if (rs == null) {
            throw new IllegalArgumentException("Unknown RNG stream: " + name);
        }
        return rs;
    }

    /**
     * Get the raw {@link Well19937c} for backward-compatible callers.
     *
     * @deprecated prefer {@link #stream(String)} which returns a
     *             convenience {@link RngStream}.
     */
    @Deprecated
    public Well19937c getStream(String name) {
        Well19937c rng = rawStreams.get(name);
        if (rng == null) {
            // Try case-insensitive lookup for backward compat
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            rng = rawStreams.get(lower);
        }
        if (rng == null) {
            throw new IllegalArgumentException("Unknown RNG stream: " + name);
        }
        return rng;
    }

    // ── Convenience methods (delegate to RngStream) ────────────────

    public double nextGaussian(String streamName, double mean, double sigma) {
        return stream(streamName).nextGaussian(mean, sigma);
    }

    public double nextDouble(String streamName) {
        return stream(streamName).nextDouble();
    }

    public int nextInt(String streamName, int bound) {
        return stream(streamName).nextInt(bound);
    }

    /**
     * Threshold above which we switch from the exact Poisson sampler
     * to a fast Gaussian approximation.
     */
    private static final double POISSON_NORMAL_THRESHOLD = 1000.0;

    /**
     * Draw from a Poisson distribution with the given mean.
     */
    public int nextPoisson(String streamName, double mean) {
        return stream(streamName).nextPoisson(mean);
    }

    /**
     * Draw from an exponential distribution with the given mean.
     */
    public double nextExponential(String streamName, double mean) {
        return stream(streamName).nextExponential(mean);
    }
}
