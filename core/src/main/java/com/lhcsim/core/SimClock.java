package com.lhcsim.core;

/**
 * Simulation clock that converts fixed-step physics ticks into in-game
 * date/time according to a configurable speed multiplier.
 * <p>
 * Default rule (§14 rule 11): 1 s real-time = 2 h in-game time.
 */
public final class SimClock {

    /** Default mapping: 1 real second → 2 in-game hours (= 7200 in-game seconds). */
    public static final double DEFAULT_MULTIPLIER = 7200.0;

    private double simTimeSeconds;
    private double multiplier = DEFAULT_MULTIPLIER;
    private boolean paused;

    public SimClock() {
        this(0.0);
    }

    public SimClock(double initialSimSeconds) {
        this.simTimeSeconds = initialSimSeconds;
    }

    /**
     * Advance the clock by one physics step.
     *
     * @param dtRealSeconds real-time seconds elapsed this step
     */
    public void advance(double dtRealSeconds) {
        if (!paused) {
            simTimeSeconds += dtRealSeconds * multiplier;
        }
    }

    // ── In-game date helpers ───────────────────────────────────────

    /** Total elapsed simulation time in seconds. */
    public double getSimTimeSeconds() { return simTimeSeconds; }

    /** In-game hours since start. */
    public double getInGameHours() { return simTimeSeconds / 3600.0; }

    /** In-game year (1-based). */
    public int getYear() {
        double days = simTimeSeconds / 86400.0;
        return 1 + (int) (days / 365);
    }

    /** In-game day within the current year (1-based). */
    public int getDay() {
        double days = simTimeSeconds / 86400.0;
        return 1 + (int) (days % 365);
    }

    // ── Multiplier ─────────────────────────────────────────────────

    public double getMultiplier() { return multiplier; }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    // ── Pause ──────────────────────────────────────────────────────

    public boolean isPaused() { return paused; }

    public void setPaused(boolean paused) { this.paused = paused; }
}
