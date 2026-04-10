package com.lhcsim.physics.particles;

/**
 * Applies a Lorentz boost defined by a velocity β-vector.
 * <p>
 * The standard boost transformation for a four-vector (E, p) is:
 * <pre>
 *   E'       = γ (E − β⃗ · p⃗)
 *   p⃗_∥'    = γ (p⃗_∥ − β E)
 *   p⃗_⊥'    = p⃗_⊥
 * </pre>
 * where ∥ and ⊥ are relative to the β-vector direction.
 */
public final class LorentzBoost {

    private final double bx;
    private final double by;
    private final double bz;
    private final double beta2;
    private final double gamma;

    /**
     * Creates a Lorentz boost from β-vector components.
     *
     * @param bx x-component of β = v/c
     * @param by y-component of β
     * @param bz z-component of β
     * @throws IllegalArgumentException if |β| ≥ 1
     */
    public LorentzBoost(double bx, double by, double bz) {
        this.bx = bx;
        this.by = by;
        this.bz = bz;
        this.beta2 = bx * bx + by * by + bz * bz;
        if (beta2 >= 1.0) {
            throw new IllegalArgumentException("|beta| must be < 1, got " + Math.sqrt(beta2));
        }
        this.gamma = 1.0 / Math.sqrt(1.0 - beta2);
    }

    /**
     * Creates a boost with β = p⃗ / E from a four-momentum.
     * Applying this boost to the given four-momentum brings it to rest.
     */
    public static LorentzBoost fromFourMomentum(FourMomentum p) {
        double e = p.getE();
        if (e <= 0) {
            return new LorentzBoost(0, 0, 0);
        }
        return new LorentzBoost(p.getPx() / e, p.getPy() / e, p.getPz() / e);
    }

    /**
     * Creates the inverse boost (negative β).
     */
    public LorentzBoost inverse() {
        return new LorentzBoost(-bx, -by, -bz);
    }

    /**
     * Applies this boost to the given four-momentum.
     *
     * @param p the four-momentum to boost
     * @return boosted four-momentum
     */
    public FourMomentum apply(FourMomentum p) {
        if (beta2 < 1e-30) {
            return p; // identity boost
        }

        double e = p.getE();
        double px = p.getPx();
        double py = p.getPy();
        double pz = p.getPz();

        // β⃗ · p⃗
        double bdotp = bx * px + by * py + bz * pz;

        // Factor for the spatial part: (γ-1)/β² · (β⃗·p⃗) - γ·E
        double factor = (gamma - 1.0) / beta2 * bdotp - gamma * e;

        double newE = gamma * (e - bdotp);
        double newPx = px + factor * bx;
        double newPy = py + factor * by;
        double newPz = pz + factor * bz;

        return new FourMomentum(newE, newPx, newPy, newPz);
    }

    public double getBx() { return bx; }
    public double getBy() { return by; }
    public double getBz() { return bz; }
    public double beta() { return Math.sqrt(beta2); }
    public double gamma() { return gamma; }
}
