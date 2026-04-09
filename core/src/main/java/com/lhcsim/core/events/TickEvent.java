package com.lhcsim.core.events;

/**
 * Published every fixed-step physics tick.
 *
 * @param dt           the fixed time step in seconds (e.g. 1/120)
 * @param totalSimTime total elapsed simulation time in seconds
 */
public record TickEvent(double dt, double totalSimTime) implements Event {
}
