package com.lhcsim.core;

/**
 * Manages in-game time progression with configurable speed modes.
 * Posts {@link TimeAdvanced} and {@link YearChanged} events through the
 * {@link EventBus}.
 */
public final class TimeManager {

    public static final double NORMAL_SPEED = 2.0;
    public static final double FAST_SPEED   = 8.0;

    public enum TimeMode {
        NORMAL, FAST, REALTIME, PAUSED
    }

    // ── Event records ──────────────────────────────────────────────

    public record TimeAdvanced(double deltaHours, double totalHours, int year, int day) {}

    public record YearChanged(int newYear) {}

    // ── State ──────────────────────────────────────────────────────

    private final EventBus eventBus;
    private TimeMode mode = TimeMode.NORMAL;
    private double inGameHours;
    private int lastYear;

    public TimeManager(EventBus eventBus) {
        this(eventBus, 0.0);
    }

    public TimeManager(EventBus eventBus, double initialHours) {
        this.eventBus = eventBus;
        this.inGameHours = initialHours;
        this.lastYear = computeYear(initialHours);
    }

    /**
     * Advance the simulation clock.
     *
     * @param realDeltaSeconds wall-clock seconds since the last frame
     */
    public void update(double realDeltaSeconds) {
        if (mode == TimeMode.PAUSED) {
            return;
        }
        double speedMultiplier = switch (mode) {
            case NORMAL   -> NORMAL_SPEED;
            case FAST     -> FAST_SPEED;
            case REALTIME -> 1.0;
            case PAUSED   -> 0.0; // unreachable, kept for completeness
        };
        double deltaHours = realDeltaSeconds * speedMultiplier;
        inGameHours += deltaHours;

        int currentYear = computeYear(inGameHours);
        int currentDay  = computeDay(inGameHours);

        eventBus.post(new TimeAdvanced(deltaHours, inGameHours, currentYear, currentDay));

        if (currentYear != lastYear) {
            eventBus.post(new YearChanged(currentYear));
            lastYear = currentYear;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static int computeYear(double hours) {
        double totalDays = hours / 24.0;
        return 1 + (int) (totalDays / 365);
    }

    private static int computeDay(double hours) {
        double totalDays = hours / 24.0;
        return 1 + (int) (totalDays % 365);
    }

    // ── Accessors ──────────────────────────────────────────────────

    public TimeMode getMode() {
        return mode;
    }

    public void setMode(TimeMode mode) {
        this.mode = mode;
    }

    public double getInGameHours() {
        return inGameHours;
    }

    public void setInGameHours(double inGameHours) {
        this.inGameHours = inGameHours;
        this.lastYear = computeYear(inGameHours);
    }

    public int getYear() {
        return computeYear(inGameHours);
    }

    public int getDay() {
        return computeDay(inGameHours);
    }
}
