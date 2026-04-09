package com.lhcsim.physics.particles;

/**
 * A detector-level reconstructed physics object produced by event reconstruction.
 * <p>
 * Coordinates follow the standard collider convention:
 * η (pseudorapidity), φ (azimuthal angle), pT (transverse momentum in GeV),
 * and energy (GeV).
 */
public class ReconstructedObject {

    /** Broad classification of the reconstructed physics object. */
    public enum ObjectType {
        ELECTRON,
        MUON,
        PHOTON,
        JET,
        TAU,
        BJET,
        MET
    }

    /** Identification quality working point. */
    public enum Quality {
        LOOSE,
        MEDIUM,
        TIGHT
    }

    private final double eta;
    private final double phi;
    private final double pT;
    private final double energy;
    private final ObjectType type;
    private final Quality quality;
    private final int charge;

    public ReconstructedObject(double eta, double phi, double pT, double energy,
                               ObjectType type, Quality quality, int charge) {
        this.eta = eta;
        this.phi = phi;
        this.pT = pT;
        this.energy = energy;
        this.type = type;
        this.quality = quality;
        this.charge = charge;
    }

    /**
     * Angular distance ΔR between this object and another, defined as
     * ΔR = sqrt(Δη² + Δφ²), where Δφ is wrapped to [−π, π].
     */
    public double deltaR(ReconstructedObject other) {
        double dEta = this.eta - other.eta;
        double dPhi = this.phi - other.phi;
        // Wrap Δφ into [−π, π]
        while (dPhi > Math.PI) {
            dPhi -= 2.0 * Math.PI;
        }
        while (dPhi < -Math.PI) {
            dPhi += 2.0 * Math.PI;
        }
        return Math.sqrt(dEta * dEta + dPhi * dPhi);
    }

    // ── Getters ─────────────────────────────────────────────────────

    public double getEta() {
        return eta;
    }

    public double getPhi() {
        return phi;
    }

    public double getPT() {
        return pT;
    }

    public double getEnergy() {
        return energy;
    }

    public ObjectType getType() {
        return type;
    }

    public Quality getQuality() {
        return quality;
    }

    public int getCharge() {
        return charge;
    }

    @Override
    public String toString() {
        return type + " pT=" + String.format("%.2f", pT)
                + " η=" + String.format("%.2f", eta)
                + " φ=" + String.format("%.2f", phi)
                + " q=" + charge
                + " (" + quality + ")";
    }
}
