package com.lhcsim.physics.particles;

/**
 * Relativistic four-momentum (E, px, py, pz) with all components in GeV.
 * <p>
 * Follows the (+, −, −, −) metric signature convention commonly used in
 * high-energy physics: m² = E² − px² − py² − pz².
 */
public class FourMomentum {

    private static final double ETA_GUARD = 1e-10;

    private final double e;
    private final double px;
    private final double py;
    private final double pz;

    public FourMomentum(double e, double px, double py, double pz) {
        this.e = e;
        this.px = px;
        this.py = py;
        this.pz = pz;
    }

    // ── Factory methods ─────────────────────────────────────────────

    /**
     * Creates a four-momentum from a known mass and three-momentum components.
     * Energy is derived as E = sqrt(m² + p²).
     */
    public static FourMomentum fromMassAndMomentum(double mass, double px, double py, double pz) {
        double energy = Math.sqrt(mass * mass + px * px + py * py + pz * pz);
        return new FourMomentum(energy, px, py, pz);
    }

    /** Creates a four-momentum for a particle at rest with the given mass. */
    public static FourMomentum atRest(double mass) {
        return new FourMomentum(mass, 0, 0, 0);
    }

    // ── Kinematic quantities ────────────────────────────────────────

    /**
     * Invariant mass: m = sqrt(E² − p²).
     * Returns 0 if the squared mass is negative (numerical rounding for massless particles).
     */
    public double invariantMass() {
        double m2 = e * e - px * px - py * py - pz * pz;
        return m2 >= 0 ? Math.sqrt(m2) : 0.0;
    }

    /** Transverse momentum: pT = sqrt(px² + py²). */
    public double pT() {
        return Math.sqrt(px * px + py * py);
    }

    /** Magnitude of three-momentum: |p| = sqrt(px² + py² + pz²). */
    public double p() {
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    /**
     * Pseudorapidity: η = 0.5 · ln((|p| + pz) / (|p| − pz)).
     * Returns ±{@link Double#MAX_VALUE} when |pz| is very close to |p|.
     */
    public double eta() {
        double pMag = p();
        double diff = pMag - Math.abs(pz);
        if (diff < ETA_GUARD) {
            return pz >= 0 ? Double.MAX_VALUE : -Double.MAX_VALUE;
        }
        return 0.5 * Math.log((pMag + pz) / (pMag - pz));
    }

    /** Rapidity: y = 0.5 · ln((E + pz) / (E − pz)). */
    public double rapidity() {
        return 0.5 * Math.log((e + pz) / (e - pz));
    }

    /** Azimuthal angle φ ∈ [−π, π]. */
    public double phi() {
        return Math.atan2(py, px);
    }

    // ── Algebra ─────────────────────────────────────────────────────

    /** Component-wise addition of two four-momenta. */
    public FourMomentum add(FourMomentum other) {
        return new FourMomentum(
                this.e + other.e,
                this.px + other.px,
                this.py + other.py,
                this.pz + other.pz);
    }

    /**
     * Lorentz boost along the z-axis by the given rapidity value.
     * <pre>
     *   E'  = E · cosh(y) + pz · sinh(y)
     *   pz' = E · sinh(y) + pz · cosh(y)
     * </pre>
     */
    public FourMomentum boostZ(double rapidity) {
        double ch = Math.cosh(rapidity);
        double sh = Math.sinh(rapidity);
        double newE = e * ch + pz * sh;
        double newPz = e * sh + pz * ch;
        return new FourMomentum(newE, px, py, newPz);
    }

    // ── Getters ─────────────────────────────────────────────────────

    public double getE() {
        return e;
    }

    public double getPx() {
        return px;
    }

    public double getPy() {
        return py;
    }

    public double getPz() {
        return pz;
    }

    @Override
    public String toString() {
        return String.format("FourMomentum(E=%.4f, px=%.4f, py=%.4f, pz=%.4f)", e, px, py, pz);
    }
}
