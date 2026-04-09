package com.lhcsim.physics.collision;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Type-safe wrapper around {@link CrossSectionTable} that uses the
 * {@link Process} enum for lookups and enforces extrapolation guards.
 * <p>
 * Provides log-log interpolation between tabulated energies.
 * Throws {@link IllegalArgumentException} if the requested energy is
 * outside ±20% of the tabulated range.
 */
public class CrossSectionDatabase {

    /** Allowed fractional overshoot beyond the tabulated energy range. */
    private static final double EXTRAPOLATION_MARGIN = 0.20;

    private final CrossSectionTable table;

    /** Min/max tabulated energies per process. */
    private final EnumMap<Process, double[]> energyBounds = new EnumMap<>(Process.class);

    public CrossSectionDatabase(CrossSectionTable table) {
        this.table = table;
        cacheEnergyBounds();
    }

    /**
     * Loads the default cross-section database from the classpath.
     */
    public static CrossSectionDatabase loadDefault() throws IOException {
        return new CrossSectionDatabase(CrossSectionTable.loadDefault());
    }

    /**
     * Returns the cross-section for the given process at centre-of-mass
     * energy {@code ecmTeV}, using log-log interpolation.
     *
     * @param process the physics process
     * @param ecmTeV  centre-of-mass energy [TeV]
     * @return cross-section [pb]
     * @throws IllegalArgumentException if extrapolating beyond ±20%
     */
    public double get(Process process, double ecmTeV) {
        double[] bounds = energyBounds.get(process);
        if (bounds != null && bounds[0] > 0 && bounds[1] > 0) {
            double minAllowed = bounds[0] * (1.0 - EXTRAPOLATION_MARGIN);
            double maxAllowed = bounds[1] * (1.0 + EXTRAPOLATION_MARGIN);
            if (ecmTeV < minAllowed || ecmTeV > maxAllowed) {
                throw new IllegalArgumentException(String.format(
                        "Energy %.3f TeV is outside allowed range [%.3f, %.3f] for process %s",
                        ecmTeV, minAllowed, maxAllowed, process.key()));
            }
        }
        return table.getCrossSectionAt(process.key(), ecmTeV);
    }

    /**
     * Returns the cross-section for the given process key at {@code ecmTeV}.
     * Falls back to the underlying table without extrapolation guard.
     */
    public double getByKey(String processKey, double ecmTeV) {
        return table.getCrossSectionAt(processKey, ecmTeV);
    }

    /**
     * Expected number of events: N = sigma * L_int.
     *
     * @param process the physics process
     * @param ecmTeV  centre-of-mass energy [TeV]
     * @param lumiPb  integrated luminosity [pb^-1]
     * @return expected event count
     */
    public double expectedEvents(Process process, double ecmTeV, double lumiPb) {
        return get(process, ecmTeV) * lumiPb;
    }

    /** Returns the underlying cross-section table. */
    public CrossSectionTable getTable() {
        return table;
    }

    // ── Internal ────────────────────────────────────────────────────

    private void cacheEnergyBounds() {
        for (Process p : Process.values()) {
            CrossSectionTable.ProcessCrossSection proc = table.getProcess(p.key());
            if (proc != null && proc.getCrossSections() != null) {
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                for (String key : proc.getCrossSections().keySet()) {
                    double e = Double.parseDouble(key);
                    if (e < min) min = e;
                    if (e > max) max = e;
                }
                energyBounds.put(p, new double[]{min, max});
            }
        }
    }
}
