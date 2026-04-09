package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TransferMatrix;

/**
 * Sector dipole magnet providing horizontal bending.
 */
public class Dipole extends AcceleratorElement implements LatticeElement {

    private static final double C_LIGHT_GEV_TO_TM = 0.299792458;

    private final double field;

    /**
     * @param name   element name
     * @param length arc length [m]
     * @param field  magnetic field strength [T]
     */
    public Dipole(String name, double length, double field) {
        super(name, length);
        this.field = field;
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        double energy = lorentzGamma * Bunch.PROTON_MASS;
        double momentum = Math.sqrt(energy * energy - Bunch.PROTON_MASS * Bunch.PROTON_MASS);
        double bRho = momentum / C_LIGHT_GEV_TO_TM;
        double bendRadius = bRho / field;
        return TransferMatrix.sectorDipole(getLength(), bendRadius, lorentzGamma);
    }

    @Override
    public String getElementType() {
        return "DIPOLE";
    }

    public double getField() {
        return field;
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
        double bendRadius = brho / field;
        double momentum = brho * 0.299792458;
        double energy = Math.sqrt(momentum * momentum + Bunch.PROTON_MASS * Bunch.PROTON_MASS);
        double gamma = energy / Bunch.PROTON_MASS;
        return TransferMatrix.sectorDipole(getLength(), bendRadius, gamma);
    }

    @Override
    public ElementType type() {
        return ElementType.DIPOLE;
    }
}
