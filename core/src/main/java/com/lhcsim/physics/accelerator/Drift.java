package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Field-free drift space.
 */
public class Drift extends AcceleratorElement implements LatticeElement {

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
        double momentum = brho * 0.299792458; // GeV/c
        double energy = Math.sqrt(momentum * momentum + Bunch.PROTON_MASS * Bunch.PROTON_MASS);
        double gamma = energy / Bunch.PROTON_MASS;
        return getTransferMatrix(gamma);
    }

    @Override
    public ElementType type() {
        return ElementType.DRIFT;
    }
}
