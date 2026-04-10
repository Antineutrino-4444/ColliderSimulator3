package com.lhcsim.physics.collision;

/**
 * Accumulates instantaneous luminosity over a fill into integrated
 * luminosity, exposed in fb^-1.
 * <p>
 * 1 fb^-1 = 10^39 cm^-2 = 1000 pb^-1.
 */
public class IntegratedLuminosity {

    /** Accumulated integrated luminosity [cm^-2]. */
    private double integratedCm2;

    /** Accumulated integrated luminosity for the current fill [cm^-2]. */
    private double fillCm2;

    /** Whether a fill is currently active. */
    private boolean fillActive;

    public IntegratedLuminosity() {
        this.integratedCm2 = 0;
        this.fillCm2 = 0;
        this.fillActive = false;
    }

    /**
     * Creates an instance pre-loaded with a known value (e.g. from save game).
     */
    public IntegratedLuminosity(double totalFb) {
        this.integratedCm2 = totalFb / 1e-39;
        this.fillCm2 = 0;
        this.fillActive = false;
    }

    /** Start a new fill. Resets the per-fill counter. */
    public void startFill() {
        fillCm2 = 0;
        fillActive = true;
    }

    /** End the current fill. Per-fill counter becomes stale. */
    public void endFill() {
        fillActive = false;
    }

    /**
     * Accumulates luminosity from a time slice.
     *
     * @param instLumiCm2s instantaneous luminosity [cm^-2 s^-1]
     * @param dtSeconds    time slice [s]
     */
    public void accumulate(double instLumiCm2s, double dtSeconds) {
        double delta = instLumiCm2s * dtSeconds;
        integratedCm2 += delta;
        if (fillActive) {
            fillCm2 += delta;
        }
    }

    /** Total integrated luminosity [fb^-1]. */
    public double totalFb() {
        return integratedCm2 * 1e-39;
    }

    /** Current fill integrated luminosity [fb^-1]. */
    public double fillFb() {
        return fillCm2 * 1e-39;
    }

    /** Total integrated luminosity [pb^-1]. */
    public double totalPb() {
        return integratedCm2 * 1e-36;
    }

    /** Total integrated luminosity [cm^-2]. */
    public double totalCm2() {
        return integratedCm2;
    }

    /** Adds a known amount to the total (e.g. from loading a save). */
    public void addFb(double fb) {
        integratedCm2 += fb / 1e-39;
    }

    public boolean isFillActive() {
        return fillActive;
    }
}
