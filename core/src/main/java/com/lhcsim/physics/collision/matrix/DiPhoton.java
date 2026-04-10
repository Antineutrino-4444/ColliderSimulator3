package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;

/**
 * Matrix element for di-photon production: q q̄ → γγ.
 * <p>
 * At leading order QCD + QED:
 * <pre>
 *   |M|² = 2 · N_c · e_q⁴ · α²_em · (t̂/û + û/t̂)
 * </pre>
 * Averaged over initial colors (1/3²) and spins (1/4).
 */
public class DiPhoton implements MatrixElement {

    private static final double ALPHA_EM = 1.0 / 137.036;

    @Override
    public String processName() {
        return "qq̄ → γγ (di-photon)";
    }

    @Override
    public double squared(FourMomentum p1, FourMomentum p2, FourMomentum p3, FourMomentum p4) {
        return squaredForCharge(p1, p2, p3, p4, 2.0 / 3.0); // default u-quark
    }

    /**
     * Computes |M|² for a given quark electric charge.
     */
    public double squaredForCharge(FourMomentum p1, FourMomentum p2,
                                   FourMomentum p3, FourMomentum p4,
                                   double charge) {
        double s = p1.add(p2).mass2();
        double t = p1.subtract(p3).mass2();
        double u = p1.subtract(p4).mass2();

        if (Math.abs(t) < 1e-10 || Math.abs(u) < 1e-10) return 0;

        double eq4 = Math.pow(charge, 4);

        // Color average: 1/9, spin average: 1/4
        // N_c = 3 for the color trace
        double colorSpinAvg = 1.0 / 36.0;
        double nc = 3.0;

        return colorSpinAvg * 2.0 * nc * eq4 * ALPHA_EM * ALPHA_EM * (t / u + u / t);
    }
}
