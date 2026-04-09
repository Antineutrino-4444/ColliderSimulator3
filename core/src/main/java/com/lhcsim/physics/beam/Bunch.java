package com.lhcsim.physics.beam;

/**
 * Statistical description of a proton bunch in a circular collider.
 * <p>
 * All beam sizes and emittances refer to one-sigma RMS values.
 * The normalised emittance is invariant under acceleration; the geometric
 * (physical) emittance shrinks as 1/(βγ).
 */
public class Bunch {

    /** Proton rest-mass energy [GeV/c²]. */
    public static final double PROTON_MASS = 0.938272;

    private double numParticles;
    private double emittanceX;
    private double emittanceY;
    private double sigmaZ;
    private double sigmaE;
    private double energy;
    private TwissParameters twiss;

    /**
     * Full constructor with LHC-nominal defaults suggested in the parameter descriptions.
     *
     * @param numParticles bunch population (~1.15 × 10¹¹)
     * @param emittanceX   normalised horizontal emittance [m·rad] (~3.75 × 10⁻⁶)
     * @param emittanceY   normalised vertical emittance [m·rad] (~3.75 × 10⁻⁶)
     * @param sigmaZ       RMS bunch length [m] (~0.075)
     * @param sigmaE       relative RMS energy spread (~1.13 × 10⁻⁴)
     * @param energy       beam energy [GeV]
     * @param twiss        Courant–Snyder parameters at the observation point
     */
    public Bunch(double numParticles, double emittanceX, double emittanceY,
                 double sigmaZ, double sigmaE, double energy,
                 TwissParameters twiss) {
        this.numParticles = numParticles;
        this.emittanceX = emittanceX;
        this.emittanceY = emittanceY;
        this.sigmaZ = sigmaZ;
        this.sigmaE = sigmaE;
        this.energy = energy;
        this.twiss = twiss;
    }

    // ── Relativistic helpers ────────────────────────────────────────

    /** Lorentz gamma factor: γ = E / m_p. */
    public double lorentzGamma() {
        return energy / PROTON_MASS;
    }

    /** Lorentz beta factor: β = √(1 − 1/γ²). */
    public double lorentzBeta() {
        double gamma = lorentzGamma();
        return Math.sqrt(1.0 - 1.0 / (gamma * gamma));
    }

    // ── Emittance / beam size ───────────────────────────────────────

    /** Geometric (physical) horizontal emittance: ε_x = ε_n / (βγ). */
    public double geometricEmittanceX() {
        return emittanceX / (lorentzBeta() * lorentzGamma());
    }

    /** Geometric (physical) vertical emittance: ε_y = ε_n / (βγ). */
    public double geometricEmittanceY() {
        return emittanceY / (lorentzBeta() * lorentzGamma());
    }

    /** RMS horizontal beam size: σ_x = √(ε_x · β_x). */
    public double beamSizeX() {
        return Math.sqrt(geometricEmittanceX() * twiss.getBetaX());
    }

    /** RMS vertical beam size: σ_y = √(ε_y · β_y). */
    public double beamSizeY() {
        return Math.sqrt(geometricEmittanceY() * twiss.getBetaY());
    }

    /**
     * RMS horizontal beam size at the interaction point.
     * Alias for {@link #beamSizeX()} when the Twiss stored in this bunch
     * corresponds to the IP optics (i.e. β* is set).
     */
    public double sigmaXAtIp() {
        return beamSizeX();
    }

    /**
     * RMS vertical beam size at the interaction point.
     * Alias for {@link #beamSizeY()} when the Twiss stored in this bunch
     * corresponds to the IP optics.
     */
    public double sigmaYAtIp() {
        return beamSizeY();
    }

    /**
     * Magnetic rigidity Bρ = p / (c · 10⁻⁹) expressed in T·m, where
     * p = √(E² − m²) [GeV/c] and the constant 0.299792458 converts
     * GeV/c to T·m.
     */
    public double magneticRigidity() {
        double p = Math.sqrt(energy * energy - PROTON_MASS * PROTON_MASS);
        return p / 0.299792458;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public double getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(double numParticles) {
        this.numParticles = numParticles;
    }

    public double getEmittanceX() {
        return emittanceX;
    }

    public void setEmittanceX(double emittanceX) {
        this.emittanceX = emittanceX;
    }

    public double getEmittanceY() {
        return emittanceY;
    }

    public void setEmittanceY(double emittanceY) {
        this.emittanceY = emittanceY;
    }

    public double getSigmaZ() {
        return sigmaZ;
    }

    public void setSigmaZ(double sigmaZ) {
        this.sigmaZ = sigmaZ;
    }

    public double getSigmaE() {
        return sigmaE;
    }

    public void setSigmaE(double sigmaE) {
        this.sigmaE = sigmaE;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public TwissParameters getTwiss() {
        return twiss;
    }

    public void setTwiss(TwissParameters twiss) {
        this.twiss = twiss;
    }

    @Override
    public String toString() {
        return String.format(
                "Bunch(N=%.3e, εx=%.3e, εy=%.3e, σz=%.4f m, σe=%.3e, E=%.2f GeV, γ=%.1f)",
                numParticles, emittanceX, emittanceY, sigmaZ, sigmaE, energy, lorentzGamma());
    }
}
