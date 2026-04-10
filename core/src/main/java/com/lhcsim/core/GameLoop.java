package com.lhcsim.core;

import com.lhcsim.core.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-step game loop that decouples the physics tick (120 Hz) from the
 * render tick (variable, typically 60 fps).
 * <p>
 * Each call to {@link #update(float)} accumulates real-world time and
 * steps the physics in fixed {@code 1/120 s} increments, with a maximum
 * of {@value #MAX_CATCH_UP_STEPS} catch-up steps per frame to avoid a
 * death spiral.
 * <p>
 * A {@link TickEvent} is published via the {@link EventBus} for every
 * physics step.
 */
public final class GameLoop {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);

    /** Fixed physics step: 1/120 s. */
    public static final double FIXED_DT = 1.0 / 120.0;

    /** Maximum catch-up steps per frame to prevent spiral. */
    public static final int MAX_CATCH_UP_STEPS = 4;

    private final EventBus eventBus;
    private final SimClock simClock;
    private final List<Tickable> tickables = new ArrayList<>();

    private double accumulator;
    private double totalSimTime;
    private int physicsStepsThisFrame;

    // ── Debug counters ─────────────────────────────────────────────
    private int physicsTicksLastSecond;
    private int physicsTickCounter;
    private float fpsTimer;

    public GameLoop(EventBus eventBus, SimClock simClock) {
        this.eventBus = eventBus;
        this.simClock = simClock;
    }

    /** Register a system that should be stepped every physics tick. */
    public void addTickable(Tickable tickable) {
        tickables.add(tickable);
    }

    /** Remove a previously registered tickable. */
    public void removeTickable(Tickable tickable) {
        tickables.remove(tickable);
    }

    /**
     * Called once per frame from the LibGDX render loop.
     *
     * @param realDeltaSeconds wall-clock seconds since the last frame
     */
    public void update(float realDeltaSeconds) {
        // Drain deferred events
        eventBus.drainAsync();

        // Don't step physics when paused
        if (simClock.isPaused()) {
            return;
        }

        accumulator += realDeltaSeconds;
        physicsStepsThisFrame = 0;

        while (accumulator >= FIXED_DT && physicsStepsThisFrame < MAX_CATCH_UP_STEPS) {
            stepPhysics();
            accumulator -= FIXED_DT;
            physicsStepsThisFrame++;
        }

        // If still behind, drop the remainder to avoid spiral
        if (accumulator > FIXED_DT * MAX_CATCH_UP_STEPS) {
            LOG.debug("Dropping {} s of accumulated time to prevent spiral",
                    accumulator - FIXED_DT);
            accumulator = 0;
        }

        // fps / tps tracking
        fpsTimer += realDeltaSeconds;
        if (fpsTimer >= 1.0f) {
            physicsTicksLastSecond = physicsTickCounter;
            physicsTickCounter = 0;
            fpsTimer -= 1.0f;
        }
    }

    private void stepPhysics() {
        simClock.advance(FIXED_DT);
        totalSimTime += FIXED_DT;

        for (Tickable t : tickables) {
            t.tick(FIXED_DT);
        }

        eventBus.post(new TickEvent(FIXED_DT, totalSimTime));
        physicsTickCounter++;
    }

    // ── Debug info ─────────────────────────────────────────────────

    /** Physics ticks completed in the last wall-clock second (~120). */
    public int getPhysicsTicksPerSecond() {
        return physicsTicksLastSecond;
    }

    /** Number of physics steps taken in the most recent frame. */
    public int getPhysicsStepsThisFrame() {
        return physicsStepsThisFrame;
    }

    public double getTotalSimTime() {
        return totalSimTime;
    }
}
