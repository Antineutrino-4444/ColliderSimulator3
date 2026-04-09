package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Beam position monitor — a zero-length diagnostic device.
 * <p>
 * Has no effect on the optics (identity transfer matrix) but can be
 * addressed by name for readout in the control room.
 */
public class BeamPositionMonitor extends AcceleratorElement implements LatticeElement {

    /**
     * @param name unique element identifier
     */
    public BeamPositionMonitor(String name) {
        super(name, 0.0);
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        return TransferMatrix.identity();
    }

    @Override
    public String getElementType() {
        return "BPM";
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
        return ElementType.BPM;
    }
}
