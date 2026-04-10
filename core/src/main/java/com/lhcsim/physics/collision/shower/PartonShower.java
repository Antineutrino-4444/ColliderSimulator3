package com.lhcsim.physics.collision.shower;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.particles.FourMomentum;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified angular-ordered parton shower.
 * <p>
 * Evolves each colored outgoing parton from the hard scale Q² down to a cutoff
 * Q₀² = 1 GeV², using DGLAP splitting functions and Sudakov form factors.
 * <p>
 * Splitting functions implemented:
 * <ul>
 *   <li>P_qq(z) = C_F · (1 + z²) / (1 - z)</li>
 *   <li>P_gg(z) = C_A · [z/(1-z) + (1-z)/z + z(1-z)]</li>
 *   <li>P_qg(z) = T_R · [z² + (1-z)²]</li>
 * </ul>
 * Uses one-loop running α_s(Q²) with Λ_QCD = 0.2 GeV.
 */
public class PartonShower {

    /** Infrared cutoff scale [GeV²]. */
    private static final double Q0_SQUARED = 1.0;

    /** QCD color factors. */
    private static final double CF = 4.0 / 3.0;
    private static final double CA = 3.0;
    private static final double TR = 0.5;

    /** Λ_QCD² for running coupling. */
    private static final double LAMBDA_QCD2 = 0.04; // (0.2 GeV)²

    /** Maximum number of emissions per parton. */
    private static final int MAX_EMISSIONS = 50;

    private final RngStream rng;

    /**
     * A showered parton with its four-momentum and PDG ID.
     */
    public record ShowerParton(int pdgId, FourMomentum momentum) {}

    public PartonShower(RngStream rng) {
        this.rng = rng;
    }

    /**
     * Showers a single hard parton, returning the list of final partons
     * after the cascade terminates at Q₀².
     *
     * @param pdgId    PDG ID of the parton (21 for gluon, ±1-6 for quarks)
     * @param momentum four-momentum of the hard parton
     * @param q2Start  starting scale Q² [GeV²]
     * @return list of showered partons
     */
    public List<ShowerParton> shower(int pdgId, FourMomentum momentum, double q2Start) {
        List<ShowerParton> result = new ArrayList<>();
        evolve(pdgId, momentum, q2Start, result, 0);
        return result;
    }

    /**
     * Showers a list of hard partons.
     */
    public List<ShowerParton> showerAll(List<ShowerParton> hardPartons, double q2Start) {
        List<ShowerParton> result = new ArrayList<>();
        for (ShowerParton p : hardPartons) {
            result.addAll(shower(p.pdgId(), p.momentum(), q2Start));
        }
        return result;
    }

    private void evolve(int pdgId, FourMomentum momentum, double q2,
                        List<ShowerParton> result, int nEmissions) {
        if (q2 <= Q0_SQUARED || nEmissions >= MAX_EMISSIONS) {
            result.add(new ShowerParton(pdgId, momentum));
            return;
        }

        // Generate next emission scale using veto algorithm
        double q2Next = generateNextScale(q2, pdgId);

        if (q2Next <= Q0_SQUARED) {
            // No more emissions
            result.add(new ShowerParton(pdgId, momentum));
            return;
        }

        // Sample splitting fraction z
        double z = sampleZ(pdgId);

        // Determine emission type and daughter PDG IDs
        int[] daughters = getDaughterPdgIds(pdgId);

        // Split the momentum
        // Daughter 1 gets fraction z, daughter 2 gets (1-z)
        double eMom = momentum.getE();
        if (eMom < 0.5) {
            // Too soft to split
            result.add(new ShowerParton(pdgId, momentum));
            return;
        }

        // Generate a small transverse kick
        double kT = Math.sqrt(q2Next) * Math.sqrt(z * (1 - z));
        double phiEmission = 2.0 * Math.PI * rng.nextDouble();

        // Simple collinear splitting along the parent direction
        double pMag = momentum.p();
        if (pMag < 1e-10) {
            result.add(new ShowerParton(pdgId, momentum));
            return;
        }

        double nx = momentum.getPx() / pMag;
        double ny = momentum.getPy() / pMag;
        double nz = momentum.getPz() / pMag;

        // Perpendicular vectors
        double[] perp1 = perpVector(nx, ny, nz);
        double[] perp2 = {ny * perp1[2] - nz * perp1[1],
                nz * perp1[0] - nx * perp1[2],
                nx * perp1[1] - ny * perp1[0]};

        double ktx = kT * (Math.cos(phiEmission) * perp1[0] + Math.sin(phiEmission) * perp2[0]);
        double kty = kT * (Math.cos(phiEmission) * perp1[1] + Math.sin(phiEmission) * perp2[1]);
        double ktz = kT * (Math.cos(phiEmission) * perp1[2] + Math.sin(phiEmission) * perp2[2]);

        // Daughter 1: fraction z of parent + kT
        double p1Long = z * pMag;
        double px1 = p1Long * nx + ktx;
        double py1 = p1Long * ny + kty;
        double pz1 = p1Long * nz + ktz;
        double e1 = Math.sqrt(px1 * px1 + py1 * py1 + pz1 * pz1);
        FourMomentum mom1 = new FourMomentum(e1, px1, py1, pz1);

        // Daughter 2: fraction (1-z) of parent - kT
        double p2Long = (1 - z) * pMag;
        double px2 = p2Long * nx - ktx;
        double py2 = p2Long * ny - kty;
        double pz2 = p2Long * nz - ktz;
        double e2 = Math.sqrt(px2 * px2 + py2 * py2 + pz2 * pz2);
        FourMomentum mom2 = new FourMomentum(e2, px2, py2, pz2);

        // Recurse on both daughters
        evolve(daughters[0], mom1, q2Next, result, nEmissions + 1);
        evolve(daughters[1], mom2, q2Next, result, nEmissions + 1);
    }

    /**
     * Generates the next emission scale using a Sudakov-like veto algorithm.
     */
    private double generateNextScale(double q2, int pdgId) {
        double alphaMax = alphaS(Q0_SQUARED * 2); // overestimate
        double splittingMax = isGluon(pdgId) ? CA * 6.0 : CF * 4.0;
        double rMax = alphaMax * splittingMax / (2.0 * Math.PI);

        double t = q2;
        while (t > Q0_SQUARED) {
            // Generate next scale
            double r = rng.nextDouble();
            if (r <= 0) break;
            double dt = -Math.log(r) / rMax;
            double lnT = Math.log(t);
            t = Math.exp(lnT - dt);

            if (t <= Q0_SQUARED) break;

            // Veto: accept with probability α_s(t) · P(z) / overestimate
            double acceptProb = alphaS(t) / alphaMax;
            if (rng.nextDouble() < acceptProb) {
                return t;
            }
        }
        return 0; // below cutoff
    }

    /**
     * Samples the splitting fraction z from the appropriate splitting function.
     */
    private double sampleZ(int pdgId) {
        double zMin = 0.01;
        double zMax = 0.99;

        if (isGluon(pdgId)) {
            // P_gg: sample roughly as 1/z + 1/(1-z)
            if (rng.nextDouble() < 0.5) {
                // Sample from 1/z
                return zMin * Math.pow(zMax / zMin, rng.nextDouble());
            } else {
                // Sample from 1/(1-z)
                return 1.0 - zMin * Math.pow(zMax / zMin, rng.nextDouble());
            }
        } else {
            // P_qq: sample from (1+z^2)/(1-z) ≈ 2/(1-z)
            double u = rng.nextDouble();
            return 1.0 - (1.0 - zMin) * Math.pow((1.0 - zMax) / (1.0 - zMin), u);
        }
    }

    /**
     * Returns the PDG IDs of daughters for a splitting.
     */
    private int[] getDaughterPdgIds(int pdgId) {
        if (isGluon(pdgId)) {
            // g → gg (dominant) or g → qqbar
            if (rng.nextDouble() < 0.85) {
                return new int[]{21, 21};
            } else {
                // g → qqbar: sample light flavors
                int q = rng.nextInt(3) + 1; // u, d, s
                return new int[]{q, -q};
            }
        } else {
            // q → qg
            return new int[]{pdgId, 21};
        }
    }

    /**
     * One-loop running α_s(Q²) with 5 active flavors.
     */
    static double alphaS(double q2) {
        if (q2 <= LAMBDA_QCD2) return 0.5;
        int nf = 5;
        double b0 = (33.0 - 2.0 * nf) / (12.0 * Math.PI);
        return Math.min(0.5, 1.0 / (b0 * Math.log(q2 / LAMBDA_QCD2)));
    }

    private static boolean isGluon(int pdgId) {
        return pdgId == 21;
    }

    /**
     * Returns a vector perpendicular to (nx, ny, nz).
     */
    private static double[] perpVector(double nx, double ny, double nz) {
        double[] perp;
        if (Math.abs(nx) < 0.9) {
            perp = new double[]{0, -nz, ny};
        } else {
            perp = new double[]{-nz, 0, nx};
        }
        double norm = Math.sqrt(perp[0] * perp[0] + perp[1] * perp[1] + perp[2] * perp[2]);
        if (norm > 0) {
            perp[0] /= norm;
            perp[1] /= norm;
            perp[2] /= norm;
        }
        return perp;
    }
}
