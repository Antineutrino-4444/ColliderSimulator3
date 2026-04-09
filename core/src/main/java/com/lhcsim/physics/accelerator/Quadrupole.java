package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Thick quadrupole magnet (focusing or defocusing depending on gradient sign).
 */
public class Quadrupole extends AcceleratorElement implements LatticeElement {

    private static final double C_LIGHT_GEV_TO_TM = 0.299792458;

    private final double gradient;

    /**
     * @param name     element name
     * @param length   magnet length [m]
     * @param gradient field gradient ∂B_y/∂x [T/m]
     */
    public Quadrupole(String name, double length, double gradient) {
        super(name, length);
        this.gradient = gradient;
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        double energy = lorentzGamma * Bunch.PROTON_MASS;
        double momentum = Math.sqrt(energy * energy - Bunch.PROTON_MASS * Bunch.PROTON_MASS);
        double bRho = momentum / C_LIGHT_GEV_TO_TM;
        double k = gradient / bRho;
        return TransferMatrix.thickQuadrupole(getLength(), k, lorentzGamma);
    }

    @Override
    public String getElementType() {
        return "QUAD";
    }

    public double getGradient() {
        return gradient;
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
        double k = gradient / brho;
        double momentum = brho * 0.299792458;
        double energy = Math.sqrt(momentum * momentum + Bunch.PROTON_MASS * Bunch.PROTON_MASS);
        double gamma = energy / Bunch.PROTON_MASS;
        return TransferMatrix.thickQuadrupole(getLength(), k, gamma);
    }

    @Override
    public ElementType type() {
        return ElementType.QUADRUPOLE;
    }
}
