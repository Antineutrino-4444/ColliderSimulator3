package com.lhcsim.game.controlroom;

import java.util.List;

/**
 * Tracks the operational state of a single accelerator/detector subsystem
 * displayed in the control-room view.
 */
public class SubsystemStatus {

    /** Standard subsystem names for LHC operation. */
    public static final List<String> SUBSYSTEM_NAMES = List.of(
            "Cryogenics",
            "Vacuum",
            "Magnet Power Converters",
            "RF System",
            "Collimators",
            "Beam Dump",
            "Trigger",
            "DAQ"
    );

    /** Operational state of a subsystem. */
    public enum State {
        NOMINAL,
        WARNING,
        CRITICAL,
        OFFLINE
    }

    private final String name;
    private State state;
    private double healthPercent;
    private String statusMessage;

    public SubsystemStatus(String name) {
        this.name = name;
        this.healthPercent = 100.0;
        this.state = State.NOMINAL;
        this.statusMessage = "All systems nominal";
    }

    /**
     * Sets health and automatically derives the operational state:
     * <ul>
     *   <li>&gt; 80 → NOMINAL</li>
     *   <li>&gt; 40 → WARNING</li>
     *   <li>&gt; 0  → CRITICAL</li>
     *   <li>≤ 0    → OFFLINE</li>
     * </ul>
     */
    public void setHealthPercent(double healthPercent) {
        this.healthPercent = Math.max(0, Math.min(100, healthPercent));
        if (this.healthPercent > 80) {
            this.state = State.NOMINAL;
        } else if (this.healthPercent > 40) {
            this.state = State.WARNING;
        } else if (this.healthPercent > 0) {
            this.state = State.CRITICAL;
        } else {
            this.state = State.OFFLINE;
        }
    }

    // ── Getters & setters ───────────────────────────────────────────

    public String getName()           { return name; }
    public State getState()           { return state; }
    public double getHealthPercent()  { return healthPercent; }
    public String getStatusMessage()  { return statusMessage; }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return name + " [" + state + " " + String.format("%.0f", healthPercent) + "%]";
    }
}
