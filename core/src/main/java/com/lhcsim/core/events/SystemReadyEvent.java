package com.lhcsim.core.events;

/**
 * Published once during startup when all core systems have finished
 * initialisation and are ready to receive ticks.
 */
public record SystemReadyEvent() implements Event {
}
