package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Abstract base class for a single beam-line element in a circular accelerator.
 * <p>
 * Every element has a name, a physical length, an s-position along the ring,
 * and an active flag.  Sub-classes provide their specific transfer matrix.
 */
public abstract class AcceleratorElement {

    private final String name;
    private final double length;
    private double sPosition;
    private boolean active = true;

    /**
     * @param name   unique element identifier
     * @param length physical length [m]
     */
    protected AcceleratorElement(String name, double length) {
        this.name = name;
        this.length = length;
    }

    /**
     * Constructs the 6×6 linear transfer matrix for this element.
     *
     * @param lorentzGamma Lorentz γ of the reference particle
     * @return the transfer matrix
     */
    public abstract TransferMatrix getTransferMatrix(double lorentzGamma);

    /** Returns a short string identifying the element type (e.g. "DRIFT", "QUAD"). */
    public abstract String getElementType();

    // ── Getters / Setters ───────────────────────────────────────────

    public String getName() {
        return name;
    }

    public double getLength() {
        return length;
    }

    public double getSPosition() {
        return sPosition;
    }

    public void setSPosition(double sPosition) {
        this.sPosition = sPosition;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] L=%.4f m  s=%.4f m  %s",
                getElementType(), name, length, sPosition,
                active ? "ON" : "OFF");
    }
}
