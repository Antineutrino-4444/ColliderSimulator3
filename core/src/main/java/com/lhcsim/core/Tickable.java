package com.lhcsim.core;

/**
 * Interface for systems that participate in the fixed-step physics loop.
 * Implementations register themselves with the {@link GameLoop}.
 */
@FunctionalInterface
public interface Tickable {

    /**
     * Called once per fixed physics step.
     *
     * @param dtSeconds the fixed time step in seconds (e.g. 1/120)
     */
    void tick(double dtSeconds);
}
