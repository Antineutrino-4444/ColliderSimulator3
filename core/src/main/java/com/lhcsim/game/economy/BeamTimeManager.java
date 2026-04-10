package com.lhcsim.game.economy;

import com.lhcsim.core.EventBus;
import com.lhcsim.game.campaign.Era;

/**
 * Manages the beam-time economy: each era grants a yearly budget of beam
 * hours that the player spends on data-taking and must manage carefully.
 */
public class BeamTimeManager {

    // ── EventBus event records ──────────────────────────────────────

    public record BeamTimeChanged(double remaining, double total) {}
    public record YearBudgetReset(double newBudget) {}

    // ── State ───────────────────────────────────────────────────────

    private final EventBus eventBus;
    private double totalBudget;
    private double remaining;
    private double penaltyMultiplier = 1.0;

    public BeamTimeManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Configures the yearly beam-time budget from the given era.
     */
    public void setEra(Era era) {
        this.totalBudget = era.beamTimePerYear();
        this.remaining = totalBudget;
        eventBus.post(new BeamTimeChanged(remaining, totalBudget));
    }

    /**
     * Resets the remaining budget to the full yearly allocation.
     */
    public void resetYear() {
        this.remaining = totalBudget;
        eventBus.post(new YearBudgetReset(totalBudget));
        eventBus.post(new BeamTimeChanged(remaining, totalBudget));
    }

    /**
     * Consumes beam time (in hours), scaled by the current penalty multiplier.
     *
     * @param hours raw hours to consume
     * @return actual hours consumed (may differ due to penalty)
     */
    public double consumeTime(double hours) {
        double actual = hours * penaltyMultiplier;
        remaining = Math.max(0, remaining - actual);
        eventBus.post(new BeamTimeChanged(remaining, totalBudget));
        return actual;
    }

    /**
     * Applies a penalty multiplier (&gt; 1.0 makes beam-time more expensive).
     */
    public void applyPenalty(double multiplier) {
        this.penaltyMultiplier = Math.max(1.0, multiplier);
    }

    /** Resets the penalty multiplier to 1.0 (no penalty). */
    public void clearPenalty() {
        this.penaltyMultiplier = 1.0;
    }

    // ── Getters ─────────────────────────────────────────────────────

    public double getTotalBudget()       { return totalBudget; }
    public double getRemaining()         { return remaining; }
    public double getPenaltyMultiplier() { return penaltyMultiplier; }

    public boolean isExhausted() {
        return remaining <= 0;
    }
}
