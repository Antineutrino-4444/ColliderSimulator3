package com.lhcsim.physics.collision;

import java.util.List;

/**
 * A single bunch crossing at an interaction point.
 *
 * @param crossingId   monotonically increasing crossing identifier
 * @param timestamp    simulation time of this crossing [s]
 * @param nInelastic   number of inelastic (pileup) interactions in this crossing
 * @param hardProcesses list of hard-scatter processes that occurred in this crossing
 */
public record BunchCrossing(
        long crossingId,
        double timestamp,
        int nInelastic,
        List<Process> hardProcesses
) {}
