package com.lhcsim.core;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.Well19937c;

/**
 * Named random-number stream wrapping a {@link Well19937c} instance.
 * <p>
 * Provides convenient methods for common distributions used in the
 * physics simulation.
 */
public final class RngStream {

    /** Threshold above which Poisson sampling uses a Gaussian approximation. */
    private static final double POISSON_NORMAL_THRESHOLD = 1000.0;

    private final String name;
    private final Well19937c rng;

    public RngStream(String name, long seed) {
        this.name = name;
        this.rng  = new Well19937c(seed);
    }

    /** Stream name (e.g. "events", "optics"). */
    public String getName() { return name; }

    /** The underlying Well19937c RNG. */
    public Well19937c getRng() { return rng; }

    /** Uniform double in [0, 1). */
    public double nextDouble() {
        return rng.nextDouble();
    }

    /** Standard-normal variate. */
    public double nextGaussian() {
        return rng.nextGaussian();
    }

    /** Gaussian with given mean and sigma. */
    public double nextGaussian(double mean, double sigma) {
        return mean + rng.nextGaussian() * sigma;
    }

    /** Uniform integer in [0, bound). */
    public int nextInt(int bound) {
        return rng.nextInt(bound);
    }

    /**
     * Poisson variate.  Uses a Gaussian approximation for large means
     * (λ > {@value #POISSON_NORMAL_THRESHOLD}) because the exact sampler
     * is prohibitively slow for typical LHC event rates.
     */
    public int nextPoisson(double mean) {
        if (mean <= 0.0) return 0;
        if (mean >= POISSON_NORMAL_THRESHOLD) {
            double sample = mean + Math.sqrt(mean) * rng.nextGaussian();
            return Math.max(0, (int) Math.round(sample));
        }
        PoissonDistribution pd = new PoissonDistribution(
                rng, mean,
                PoissonDistribution.DEFAULT_EPSILON,
                PoissonDistribution.DEFAULT_MAX_ITERATIONS);
        return pd.sample();
    }

    /**
     * Exponential variate with the given mean, guarding against log(0).
     */
    public double nextExponential(double mean) {
        double u;
        do { u = rng.nextDouble(); } while (u == 0.0);
        return -mean * Math.log(u);
    }
}
