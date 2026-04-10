package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Interface for a single beam-line element in a circular accelerator.
 * <p>
 * Every element has a name, a physical length, and can produce its
 * 6×6 linear transfer matrix given the beam rigidity B*rho.
 */
public interface LatticeElement {

    /** Returns the unique element identifier. */
    String name();

    /** Returns the physical length [m]. */
    double length();

    /**
     * Constructs the 6×6 linear transfer matrix for this element.
     *
     * @param brho magnetic rigidity B*rho = p/e [T·m]
     * @return the transfer matrix
     */
    TransferMatrix matrix(double brho);

    /** Returns a short string identifying the element type (e.g. "DRIFT", "QUAD"). */
    ElementType type();

    /**
     * Element type enumeration.
     */
    enum ElementType {
        DRIFT, QUADRUPOLE, DIPOLE, SEXTUPOLE, RFCAVITY, BPM, MARKER
    }
}
