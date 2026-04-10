package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;
import com.lhcsim.physics.beam.TwissParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete accelerator ring lattice composed of {@link AcceleratorElement}s.
 * <p>
 * Provides methods for computing the one-turn transfer matrix, propagating
 * Twiss parameters around the ring, and extracting betatron tunes.
 */
public class Lattice {

    private final String name;
    private final double circumference;
    private final List<AcceleratorElement> elements = new ArrayList<>();

    private TransferMatrix cachedOneTurnMatrix;
    private double cachedGamma = Double.NaN;

    /**
     * @param name          lattice / ring name
     * @param circumference design circumference [m]
     */
    public Lattice(String name, double circumference) {
        this.name = name;
        this.circumference = circumference;
    }

    /**
     * Appends an element to the lattice, setting its s-position to the
     * current total length, and invalidates the cached one-turn matrix.
     */
    public void addElement(AcceleratorElement element) {
        double currentEnd = elements.isEmpty() ? 0.0
                : elements.get(elements.size() - 1).getSPosition()
                  + elements.get(elements.size() - 1).getLength();
        element.setSPosition(currentEnd);
        elements.add(element);
        invalidateCache();
    }

    /**
     * Computes (and caches) the one-turn transfer matrix by multiplying
     * all active element matrices in order.
     *
     * @param gamma Lorentz γ of the reference particle
     * @return the one-turn 6×6 transfer matrix
     */
    public TransferMatrix computeOneTurnMatrix(double gamma) {
        if (cachedOneTurnMatrix != null && gamma == cachedGamma) {
            return cachedOneTurnMatrix;
        }
        TransferMatrix m = TransferMatrix.identity();
        for (AcceleratorElement el : elements) {
            if (el.isActive()) {
                m = el.getTransferMatrix(gamma).multiply(m);
            }
        }
        cachedOneTurnMatrix = m;
        cachedGamma = gamma;
        return m;
    }

    /**
     * Propagates Twiss parameters through every active element, returning
     * the optics at the exit of each element.
     *
     * @param initial starting Twiss parameters
     * @param gamma   Lorentz γ of the reference particle
     * @return ordered list of {@link TwissAtS} records
     */
    public List<TwissAtS> computeTwissAlong(TwissParameters initial, double gamma) {
        List<TwissAtS> result = new ArrayList<>();
        TwissParameters current = initial;
        for (AcceleratorElement el : elements) {
            if (el.isActive()) {
                current = el.getTransferMatrix(gamma).transportTwiss(current);
            }
            double sExit = el.getSPosition() + el.getLength();
            result.add(new TwissAtS(sExit, current));
        }
        return result;
    }

    /**
     * Computes the horizontal betatron tune from the one-turn matrix trace:
     * Q_x = arccos(½ · (M₁₁ + M₂₂)) / (2π).
     */
    public double computeTuneX(double gamma) {
        TransferMatrix m = computeOneTurnMatrix(gamma);
        double trace = m.get(0, 0) + m.get(1, 1);
        return Math.acos(0.5 * trace) / (2.0 * Math.PI);
    }

    /**
     * Computes the vertical betatron tune from the one-turn matrix trace:
     * Q_y = arccos(½ · (M₃₃ + M₄₄)) / (2π).
     */
    public double computeTuneY(double gamma) {
        TransferMatrix m = computeOneTurnMatrix(gamma);
        double trace = m.get(2, 2) + m.get(3, 3);
        return Math.acos(0.5 * trace) / (2.0 * Math.PI);
    }

    private void invalidateCache() {
        cachedOneTurnMatrix = null;
        cachedGamma = Double.NaN;
    }

    // ── Getters ─────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public double getCircumference() {
        return circumference;
    }

    public List<AcceleratorElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    /**
     * Finds an element by name.
     *
     * @param elementName the element name to search for
     * @return the element, or {@code null} if not found
     */
    public AcceleratorElement findByName(String elementName) {
        for (AcceleratorElement el : elements) {
            if (el.getName().equals(elementName)) {
                return el;
            }
        }
        return null;
    }

    /**
     * Returns the total length of all elements in the lattice.
     *
     * @return total length [m]
     */
    public double totalLength() {
        double total = 0;
        for (AcceleratorElement el : elements) {
            total += el.getLength();
        }
        return total;
    }

    // ── Inner record ────────────────────────────────────────────────

    /**
     * Twiss parameters at a specific longitudinal position.
     */
    public record TwissAtS(double s, TwissParameters twiss) {}

    @Override
    public String toString() {
        return String.format("Lattice[%s] C=%.2f m  elements=%d", name, circumference, elements.size());
    }
}
