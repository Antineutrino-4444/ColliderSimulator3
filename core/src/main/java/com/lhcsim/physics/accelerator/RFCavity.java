package com.lhcsim.physics.accelerator;

import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TransferMatrix;

/**
 * RF cavity element providing longitudinal focusing.
 */
public class RFCavity extends AcceleratorElement {

    private final double voltage;
    private final double frequency;
    private final int harmonicNumber;
    private final double synchronousPhase;
    private final double circumference;

    /**
     * @param name             element name
     * @param length           physical length [m]
     * @param voltage          peak RF voltage [V]
     * @param frequency        RF frequency [Hz]
     * @param harmonicNumber   RF harmonic number h
     * @param synchronousPhase synchronous phase φ_s [rad]
     * @param circumference    ring circumference [m]
     */
    public RFCavity(String name, double length, double voltage, double frequency,
                    int harmonicNumber, double synchronousPhase, double circumference) {
        super(name, length);
        this.voltage = voltage;
        this.frequency = frequency;
        this.harmonicNumber = harmonicNumber;
        this.synchronousPhase = synchronousPhase;
        this.circumference = circumference;
    }

    @Override
    public TransferMatrix getTransferMatrix(double lorentzGamma) {
        double energy = lorentzGamma * Bunch.PROTON_MASS;
        double voltageGeV = voltage * 1e-9;
        return TransferMatrix.rfCavity(voltageGeV, harmonicNumber,
                synchronousPhase, circumference, energy, lorentzGamma);
    }

    @Override
    public String getElementType() {
        return "RFCAV";
    }

    public double getVoltage() {
        return voltage;
    }

    public double getFrequency() {
        return frequency;
    }

    public int getHarmonicNumber() {
        return harmonicNumber;
    }

    public double getSynchronousPhase() {
        return synchronousPhase;
    }

    public double getCircumference() {
        return circumference;
    }
}
