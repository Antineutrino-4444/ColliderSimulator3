package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;

/**
 * Matrix element for Higgs production via gluon-gluon fusion: gg → H.
 * <p>
 * Uses the leading-order effective coupling through a top-quark loop
 * in the heavy-top limit (Wilson coefficient form):
 * <pre>
 *   |M|² = (α_s² / (576 π²)) · (GF √2 / 2) · ŝ² · |F(τ)|²
 * </pre>
 * where F(τ) is the top-loop form factor and τ = 4mt²/ŝ.
 * <p>
 * In the heavy-top limit (mt → ∞), |F(τ)| → 2/3.
 */
public class GgHiggs implements MatrixElement {

    private static final double MT = 172.69;  // top mass [GeV]
    private static final double MH = 125.25;  // Higgs mass [GeV]
    private static final double MH2 = MH * MH;
    private static final double GF = 1.1663788e-5; // Fermi constant [GeV^-2]

    /** α_s at the Higgs mass scale. */
    private static final double ALPHA_S_MH = 0.1127;

    @Override
    public String processName() {
        return "gg → H (gluon fusion)";
    }

    @Override
    public double squared(FourMomentum p1, FourMomentum p2, FourMomentum p3, FourMomentum p4) {
        FourMomentum q = p1.add(p2);
        double sHat = q.mass2();
        if (sHat <= 0) return 0;

        // Top-loop form factor
        double tau = 4.0 * MT * MT / sHat;
        double fTau = formFactor(tau);

        // Breit-Wigner for Higgs propagator (width ~ 4.07 MeV)
        double gammaH = 0.00407; // GeV
        double bw = sHat / ((sHat - MH2) * (sHat - MH2) + MH2 * gammaH * gammaH);

        // |M|² = (α_s²/(576π²)) · GF·√2 · ŝ² · |F(τ)|² · BW
        // Color average: 1/(8×8) = 1/64, spin average: 1/4
        // With color and spin factors
        double prefactor = ALPHA_S_MH * ALPHA_S_MH * GF * Math.sqrt(2)
                / (256.0 * Math.PI * Math.PI);

        return prefactor * sHat * sHat * fTau * fTau * bw;
    }

    /**
     * Top-loop form factor |F(τ)|.
     * Uses the exact expression for τ = 4mt²/ŝ:
     * F(τ) = -2τ [1 + (1-τ) f(τ)]
     * where f(τ) = arcsin²(1/√τ) for τ ≥ 1.
     */
    private double formFactor(double tau) {
        if (tau > 1.0) {
            double f = Math.asin(1.0 / Math.sqrt(tau));
            return Math.abs(-2.0 * tau * (1.0 + (1.0 - tau) * f * f));
        } else {
            // Below threshold: use the heavy-top limit
            return 4.0 / 3.0;
        }
    }
}
