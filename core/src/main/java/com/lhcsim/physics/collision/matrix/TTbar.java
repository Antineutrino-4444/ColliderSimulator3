package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;

/**
 * Matrix element for top-quark pair production at LO QCD.
 * <p>
 * Includes both gg → tt̄ and qq̄ → tt̄ channels.
 * <p>
 * For gg → tt̄ (dominant at LHC):
 * <pre>
 *   |M|² ∝ (π²α_s²/s²) · [6(m²-t)(m²-u)/s² - (m²(s-4m²))/(3(m²-t)(m²-u))
 *           + 4/3 · ((m²-t)(m²-u) - 2m²(m²+t))/(s(m²-t))
 *           + 4/3 · ((m²-t)(m²-u) - 2m²(m²+u))/(s(m²-u))]
 * </pre>
 * For qq̄ → tt̄:
 * <pre>
 *   |M|² = (8π²α_s²)/(9s²) · (t² + u² + 2m²s)
 * </pre>
 */
public class TTbar implements MatrixElement {

    private static final double MT = 172.69;  // top mass [GeV]
    private static final double MT2 = MT * MT;

    @Override
    public String processName() {
        return "pp → tt̄ (top pair)";
    }

    @Override
    public double squared(FourMomentum p1, FourMomentum p2, FourMomentum p3, FourMomentum p4) {
        return squaredGG(p1, p2, p3, p4);
    }

    /**
     * |M|² for gg → tt̄ (spin- and color-averaged).
     */
    public double squaredGG(FourMomentum p1, FourMomentum p2,
                            FourMomentum p3, FourMomentum p4) {
        double s = p1.add(p2).mass2();
        if (s < 4 * MT2) return 0;

        double t = p1.subtract(p3).mass2();
        double u = p1.subtract(p4).mass2();

        double alphaS = runningAlphaS(s);

        double mt2_t = MT2 - t;
        double mt2_u = MT2 - u;

        if (Math.abs(mt2_t) < 1e-10 || Math.abs(mt2_u) < 1e-10) return 0;

        // Ellis-Stirling-Webber formula for gg -> tt
        double term1 = 6.0 * mt2_t * mt2_u / (s * s);
        double term2 = -MT2 * (s - 4 * MT2) / (3.0 * mt2_t * mt2_u);
        double term3 = 4.0 / 3.0 * (mt2_t * mt2_u - 2 * MT2 * (MT2 + t)) / (s * mt2_t);
        double term4 = 4.0 / 3.0 * (mt2_t * mt2_u - 2 * MT2 * (MT2 + u)) / (s * mt2_u);

        // Average over initial gluon colors (1/64) and spins (1/4)
        double colorSpinAvg = 1.0 / 256.0;

        return Math.max(0, colorSpinAvg * Math.PI * Math.PI * alphaS * alphaS
                * (term1 + term2 + term3 + term4));
    }

    /**
     * |M|² for qq̄ → tt̄ (spin- and color-averaged).
     */
    public double squaredQQ(FourMomentum p1, FourMomentum p2,
                            FourMomentum p3, FourMomentum p4) {
        double s = p1.add(p2).mass2();
        if (s < 4 * MT2) return 0;

        double t = p1.subtract(p3).mass2();
        double u = p1.subtract(p4).mass2();

        double alphaS = runningAlphaS(s);

        // 1/9 color factor (3×3̄ → 1), 1/4 spin average
        double prefactor = 8.0 * Math.PI * Math.PI * alphaS * alphaS
                / (9.0 * s * s);
        double colorSpinAvg = 1.0 / 36.0;

        return Math.max(0, colorSpinAvg * prefactor * (t * t + u * u + 2 * MT2 * s));
    }

    /**
     * One-loop running α_s(Q²) with 5 active flavors.
     */
    static double runningAlphaS(double q2) {
        double lambdaQCD2 = 0.04; // (0.2 GeV)²
        if (q2 <= lambdaQCD2) return 0.5;
        int nf = 5;
        double b0 = (33.0 - 2.0 * nf) / (12.0 * Math.PI);
        return Math.min(0.5, 1.0 / (b0 * Math.log(q2 / lambdaQCD2)));
    }
}
