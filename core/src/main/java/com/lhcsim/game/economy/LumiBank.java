package com.lhcsim.game.economy;

import com.lhcsim.physics.collision.IntegratedLuminosity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Game-wide bank of integrated luminosity, partitioned per detector.
 * <p>
 * Each interaction point / detector (ATLAS, CMS, LHCb, ALICE) has
 * its own luminosity accumulator since each IP operates at a
 * different instantaneous luminosity.
 * <p>
 * This is the game's "discovery currency" — accumulated fb^-1 unlocks
 * discovery potential.
 */
public class LumiBank {

    /** Standard LHC detector names. */
    public static final String ATLAS = "ATLAS";
    public static final String CMS   = "CMS";
    public static final String LHCB  = "LHCb";
    public static final String ALICE = "ALICE";

    private static final String[] DEFAULT_DETECTORS = { ATLAS, CMS, LHCB, ALICE };

    private final Map<String, IntegratedLuminosity> detectors = new LinkedHashMap<>();

    public LumiBank() {
        for (String det : DEFAULT_DETECTORS) {
            detectors.put(det, new IntegratedLuminosity());
        }
    }

    /**
     * Returns the accumulator for the given detector.
     *
     * @param detector detector name (e.g. "CMS")
     * @return the IntegratedLuminosity accumulator, or null if unknown
     */
    public IntegratedLuminosity get(String detector) {
        return detectors.get(detector);
    }

    /**
     * Accumulates luminosity for a specific detector.
     *
     * @param detector     detector name
     * @param instLumiCm2s instantaneous luminosity [cm^-2 s^-1]
     * @param dtSeconds    time slice [s]
     */
    public void accumulate(String detector, double instLumiCm2s, double dtSeconds) {
        IntegratedLuminosity il = detectors.get(detector);
        if (il != null) {
            il.accumulate(instLumiCm2s, dtSeconds);
        }
    }

    /**
     * Total integrated luminosity across all detectors combined [fb^-1].
     * (The main number displayed in the HUD.)
     */
    public double totalFb() {
        double sum = 0;
        for (IntegratedLuminosity il : detectors.values()) {
            sum += il.totalFb();
        }
        return sum;
    }

    /**
     * Returns an unmodifiable view of all detector accumulators.
     */
    public Map<String, IntegratedLuminosity> getDetectors() {
        return Collections.unmodifiableMap(detectors);
    }

    /** Start a fill on all detectors. */
    public void startFill() {
        for (IntegratedLuminosity il : detectors.values()) {
            il.startFill();
        }
    }

    /** End the fill on all detectors. */
    public void endFill() {
        for (IntegratedLuminosity il : detectors.values()) {
            il.endFill();
        }
    }

    // ── Save/Load helpers ──────────────────────────────────────────

    /**
     * Serializes the bank state as a Map for inclusion in a save file.
     */
    public Map<String, Double> toSaveMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Map.Entry<String, IntegratedLuminosity> e : detectors.entrySet()) {
            map.put(e.getKey(), e.getValue().totalFb());
        }
        return map;
    }

    /**
     * Restores the bank state from a save map.
     */
    public void fromSaveMap(Map<String, Double> map) {
        if (map == null) return;
        for (Map.Entry<String, Double> e : map.entrySet()) {
            IntegratedLuminosity il = detectors.get(e.getKey());
            if (il != null) {
                il.addFb(e.getValue());
            }
        }
    }
}
