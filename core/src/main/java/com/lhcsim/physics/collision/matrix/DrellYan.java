package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;

/**
 * Drell-Yan matrix element: q q̄ → Z/γ* → ℓ⁺ℓ⁻.
 * <p>
 * Uses the standard electroweak expression with Z-boson propagator:
 * <pre>
 *   |M|² ∝ (1/3) · (e_q²·e²/ŝ + g_L²·g_R² · ŝ/((ŝ-mZ²)² + mZ²·ΓZ²))
 *          × (t̂² + û²)
 * </pre>
 * Simplified form averaged over initial-state colors and spins.
 */
public class DrellYan implements MatrixElement {

    private static final double ALPHA_EM = 1.0 / 137.036;
    private static final double MZ = 91.1876; // GeV
    private static final double GZ = 2.4952;  // GeV
    private static final double MZ2 = MZ * MZ;
    private static final double SIN2W = 0.23122; // sin²θ_W
    private static final double GF = 1.1663788e-5; // Fermi constant [GeV^-2]

    // Weak coupling constants for up-type and down-type quarks
    private static final double QU = 2.0 / 3.0; // up-type electric charge
    private static final double QD = -1.0 / 3.0; // down-type electric charge

    @Override
    public String processName() {
        return "Drell-Yan (qq̄ → Z/γ* → ℓ⁺ℓ⁻)";
    }

    @Override
    public double squared(FourMomentum p1, FourMomentum p2, FourMomentum p3, FourMomentum p4) {
        return squaredForCharge(p1, p2, p3, p4, QD); // default: d-type quark
    }

    /**
     * Computes |M|² for a given quark electric charge.
     *
     * @param charge quark charge (2/3 for up-type, -1/3 for down-type)
     */
    public double squaredForCharge(FourMomentum p1, FourMomentum p2,
                                   FourMomentum p3, FourMomentum p4,
                                   double charge) {
        // Mandelstam variables
        FourMomentum q = p1.add(p2); // virtual Z/gamma
        double sHat = q.mass2();
        if (sHat <= 0) return 0;

        double tHat = p1.subtract(p3).mass2();
        double uHat = p1.subtract(p4).mass2();

        // Photon propagator contribution
        double photonProp = charge * charge * ALPHA_EM * ALPHA_EM / sHat;

        // Z propagator contribution
        double zProp = GF * MZ2 / (Math.sqrt(2) * 2 * Math.PI);
        double zDenom = (sHat - MZ2) * (sHat - MZ2) + MZ2 * GZ * GZ;
        double zContrib = zProp * zProp * sHat * sHat / zDenom;

        // Interference (simplified)
        double interference = 2.0 * charge * ALPHA_EM * zProp * sHat
                * (sHat - MZ2) / zDenom;

        // Angular factor: (t² + u²) / s²
        double angularFactor = (tHat * tHat + uHat * uHat) / (sHat * sHat);

        // Color factor 1/3 for qqbar average, factor 4π for convention
        double colorFactor = 1.0 / 3.0;

        // Result in pb-like units (GeV^-2 converted later)
        return 16.0 * Math.PI * colorFactor * (photonProp + zContrib + interference)
                * angularFactor;
    }
}
