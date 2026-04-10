package com.lhcsim.physics.beam;

import com.lhcsim.physics.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a circulating beam in a storage ring.
 * <p>
 * A beam consists of a list of bunches filling a subset of RF buckets.
 * LHC design: 2808 filled buckets out of 3564 total (25 ns spacing).
 * <p>
 * Reference: LHC Design Report Vol. 1, CERN-2004-003, Chapter 2.
 */
public class Beam {

    private final List<Bunch> bunches;
    private final int filledBuckets;
    private final int totalBuckets;
    private final double circumference;

    /** Orbit offset at IP [m] — e.g. for separation bumps. */
    private double orbitOffsetX;
    private double orbitOffsetY;

    /**
     * Creates a beam from a list of identical bunches.
     *
     * @param bunches        the filled bunches
     * @param totalBuckets   total RF buckets in the ring (LHC: 3564)
     * @param circumference  ring circumference [m] (LHC: 26658.883)
     */
    public Beam(List<Bunch> bunches, int totalBuckets, double circumference) {
        this.bunches = new ArrayList<>(bunches);
        this.filledBuckets = bunches.size();
        this.totalBuckets = totalBuckets;
        this.circumference = circumference;
    }

    /**
     * Convenience factory: creates a beam where all bunches are identical copies
     * of a template bunch.
     *
     * @param template      bunch template (will be shared, not copied — Bunch is mutable so
     *                      callers should treat it as read-only or copy it)
     * @param numBunches    number of filled buckets (LHC: 2808)
     * @param totalBuckets  total RF buckets (LHC: 3564)
     * @param circumference ring circumference [m]
     */
    public static Beam uniform(Bunch template, int numBunches,
                               int totalBuckets, double circumference) {
        List<Bunch> list = new ArrayList<>(numBunches);
        for (int i = 0; i < numBunches; i++) {
            list.add(template);
        }
        return new Beam(list, totalBuckets, circumference);
    }

    // ── Physics ────────────────────────────────────────────────────

    /**
     * Revolution frequency: f_rev = v / C where v = β·c.
     * <p>
     * Uses the first bunch's Lorentz β to determine particle speed.
     *
     * @return revolution frequency [Hz]
     */
    public double revolutionFrequency() {
        if (bunches.isEmpty()) return 0;
        double beta = bunches.get(0).lorentzBeta();
        return Constants.c * beta / circumference;
    }

    /**
     * The beam energy, taken from the first bunch [GeV].
     */
    public double energyGeV() {
        if (bunches.isEmpty()) return 0;
        return bunches.get(0).getEnergy();
    }

    /**
     * Mean number of protons per bunch across all filled bunches.
     */
    public double meanProtonsPerBunch() {
        if (bunches.isEmpty()) return 0;
        double sum = 0;
        for (Bunch b : bunches) {
            sum += b.getNumParticles();
        }
        return sum / bunches.size();
    }

    // ── Getters ────────────────────────────────────────────────────

    public List<Bunch> getBunches() {
        return Collections.unmodifiableList(bunches);
    }

    public int getFilledBuckets() {
        return filledBuckets;
    }

    public int getTotalBuckets() {
        return totalBuckets;
    }

    public double getCircumference() {
        return circumference;
    }

    public double getOrbitOffsetX() {
        return orbitOffsetX;
    }

    public void setOrbitOffsetX(double orbitOffsetX) {
        this.orbitOffsetX = orbitOffsetX;
    }

    public double getOrbitOffsetY() {
        return orbitOffsetY;
    }

    public void setOrbitOffsetY(double orbitOffsetY) {
        this.orbitOffsetY = orbitOffsetY;
    }

    @Override
    public String toString() {
        return String.format(
                "Beam(filled=%d/%d, E=%.1f GeV, f_rev=%.0f Hz, C=%.1f m)",
                filledBuckets, totalBuckets, energyGeV(), revolutionFrequency(), circumference);
    }
}
