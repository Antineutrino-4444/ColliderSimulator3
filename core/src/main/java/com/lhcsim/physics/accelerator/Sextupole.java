package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Sextupole magnet.
 * <p>
 * In the linear approximation sextupoles have no effect on the optics;
 * their contribution enters only through second-order (chromatic) terms.
 * The transfer matrix is therefore a simple drift.
 */
public class Sextupole extends AcceleratorElement {

    private final double strength;

    /**
     * @param name     element name
     * @param length   magnet length [m]
     * @param strength integrated sextupole strength [1/m²]
     */
    public Sextupole(String name, double length, double strength) {
        super(name, length);
        this.strength = strength;
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        return TransferMatrix.drift(getLength(), lorentzGamma);
    }

    @Override
    public String getElementType() {
        return "SEXT";
    }

    public double getStrength() {
        return strength;
    }
}
