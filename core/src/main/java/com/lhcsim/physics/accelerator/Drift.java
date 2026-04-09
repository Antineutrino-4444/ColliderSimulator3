package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Field-free drift space.
 */
public class Drift extends AcceleratorElement {

    public Drift(String name, double length) {
        super(name, length);
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        return TransferMatrix.drift(getLength(), lorentzGamma);
    }

    @Override
    public String getElementType() {
        return "DRIFT";
    }
}
