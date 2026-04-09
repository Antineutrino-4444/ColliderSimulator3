package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Marker element — a zero-length reference point in the lattice.
 * <p>
 * Used to denote interaction points, sector boundaries, or other
 * logical positions. Has no effect on the optics.
 */
public class Marker extends AcceleratorElement implements LatticeElement {

    /**
     * @param name unique element identifier
     */
    public Marker(String name) {
        super(name, 0.0);
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        return TransferMatrix.identity();
    }

    @Override
    public String getElementType() {
        return "MARKER";
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public double length() {
        return getLength();
    }

    @Override
    public TransferMatrix matrix(double brho) {
        return TransferMatrix.identity();
    }

    @Override
    public ElementType type() {
        return ElementType.MARKER;
    }
}
