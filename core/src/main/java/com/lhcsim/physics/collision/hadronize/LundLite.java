package com.lhcsim.physics.collision.hadronize;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.collision.shower.PartonShower;
import com.lhcsim.physics.particles.FourMomentum;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Lund-string-like hadronization model.
 * <p>
 * Converts showered partons into stable and meta-stable hadrons by:
 * <ol>
 *   <li>Pairing color-connected quarks and gluons into strings</li>
 *   <li>Iteratively splitting each string by sampling a quark-antiquark
 *       pair from the vacuum (u:d:s weights 1:1:0.3)</li>
 *   <li>Applying a Lund fragmentation function: f(z) = (1-a)·(1-z)^a / z
 *       with a ≈ 0.68</li>
 *   <li>Assigning hadron identity based on quark content</li>
 * </ol>
 * <p>
 * References: B. Andersson et al., Phys. Rep. 97 (1983) 31-145.
 */
public class LundLite {

    /** Lund fragmentation parameter. */
    private static final double LUND_A = 0.68;

    /** Minimum string mass to allow further fragmentation [GeV]. */
    private static final double MIN_STRING_MASS = 1.5;

    /** Vacuum quark weights: u:d:s = 1:1:0.3 */
    private static final double[] QUARK_WEIGHTS = {1.0, 1.0, 0.3};
    private static final double QUARK_WEIGHT_SUM = 2.3;

    /** Baryon production probability. */
    private static final double BARYON_PROB = 0.1;

    /** Pion mass [GeV]. */
    private static final double M_PI = 0.13957;
    /** Kaon mass [GeV]. */
    private static final double M_K = 0.49368;
    /** Proton mass [GeV]. */
    private static final double M_P = 0.93827;
    /** Rho mass [GeV]. */
    private static final double M_RHO = 0.77526;
    /** K* mass [GeV]. */
    private static final double M_KSTAR = 0.89166;

    private final RngStream rng;

    /**
     * A hadron produced by the hadronization process.
     */
    public record Hadron(int pdgId, FourMomentum momentum) {}

    public LundLite(RngStream rng) {
        this.rng = rng;
    }

    /**
     * Hadronizes a list of showered partons into hadrons.
     *
     * @param partons list of showered partons from the parton shower
     * @return list of produced hadrons
     */
    public List<Hadron> hadronize(List<PartonShower.ShowerParton> partons) {
        List<Hadron> hadrons = new ArrayList<>();

        if (partons.isEmpty()) return hadrons;

        // Group partons into color-connected strings
        // Simplified: pair consecutive quarks/gluons
        List<StringSystem> strings = buildStrings(partons);

        for (StringSystem s : strings) {
            hadrons.addAll(fragmentString(s));
        }

        return hadrons;
    }

    /**
     * Builds color-connected string systems from partons.
     */
    private List<StringSystem> buildStrings(List<PartonShower.ShowerParton> partons) {
        List<StringSystem> strings = new ArrayList<>();

        // Separate quarks, antiquarks, and gluons
        List<PartonShower.ShowerParton> quarks = new ArrayList<>();
        List<PartonShower.ShowerParton> antiquarks = new ArrayList<>();
        List<PartonShower.ShowerParton> gluons = new ArrayList<>();

        for (var p : partons) {
            int id = p.pdgId();
            if (id == 21) {
                gluons.add(p);
            } else if (id > 0) {
                quarks.add(p);
            } else {
                antiquarks.add(p);
            }
        }

        // Pair quarks with antiquarks, inserting gluons between them
        int nStrings = Math.max(quarks.size(), antiquarks.size());
        if (nStrings == 0) {
            // All gluons: form a gluon loop string
            if (!gluons.isEmpty()) {
                FourMomentum total = sumMomenta(gluons);
                strings.add(new StringSystem(total));
            }
            return strings;
        }

        int gIdx = 0;
        for (int i = 0; i < nStrings; i++) {
            FourMomentum total = FourMomentum.atRest(0);

            if (i < quarks.size()) {
                total = total.add(quarks.get(i).momentum());
            }
            if (i < antiquarks.size()) {
                total = total.add(antiquarks.get(i).momentum());
            }

            // Attach any available gluons to this string
            int gPerString = gluons.size() / Math.max(1, nStrings);
            for (int g = 0; g < gPerString && gIdx < gluons.size(); g++) {
                total = total.add(gluons.get(gIdx++).momentum());
            }

            strings.add(new StringSystem(total));
        }

        // Remaining gluons go to last string
        while (gIdx < gluons.size()) {
            StringSystem last = strings.get(strings.size() - 1);
            strings.set(strings.size() - 1,
                    new StringSystem(last.momentum.add(gluons.get(gIdx++).momentum())));
        }

        return strings;
    }

    /**
     * Fragments a single string system into hadrons.
     */
    private List<Hadron> fragmentString(StringSystem string) {
        List<Hadron> hadrons = new ArrayList<>();
        double stringMass = string.momentum.invariantMass();

        if (stringMass < M_PI * 2) {
            // Too light: produce a single pion
            hadrons.add(new Hadron(randomPion(), string.momentum));
            return hadrons;
        }

        // Direction of the string
        double pMag = string.momentum.p();
        double nx, ny, nz;
        if (pMag > 1e-10) {
            nx = string.momentum.getPx() / pMag;
            ny = string.momentum.getPy() / pMag;
            nz = string.momentum.getPz() / pMag;
        } else {
            nx = 0; ny = 0; nz = 1;
        }

        double remainingMass = stringMass;
        FourMomentum remainingMom = string.momentum;

        while (remainingMass > MIN_STRING_MASS) {
            // Sample z from Lund function
            double z = sampleLundZ();

            // Choose hadron type
            int hadronPdg;
            double hadronMass;
            if (rng.nextDouble() < BARYON_PROB) {
                hadronPdg = randomBaryon();
                hadronMass = M_P;
            } else {
                int vacuumQuark = sampleVacuumQuark();
                hadronPdg = assignMeson(vacuumQuark);
                hadronMass = mesonMass(hadronPdg);
            }

            double eHadron = z * remainingMom.getE();
            if (eHadron < hadronMass) {
                break;
            }

            // Momentum along string axis with small transverse kick
            double sigma_pT = 0.35; // GeV
            double pTx = rng.nextGaussian() * sigma_pT;
            double pTy = rng.nextGaussian() * sigma_pT;

            double pLong = Math.sqrt(Math.max(0, eHadron * eHadron - hadronMass * hadronMass
                    - pTx * pTx - pTy * pTy));

            // Perpendicular vectors
            double[] perp1 = perpVector(nx, ny, nz);
            double[] perp2 = cross(nx, ny, nz, perp1);

            double px = pLong * nx + pTx * perp1[0] + pTy * perp2[0];
            double py = pLong * ny + pTx * perp1[1] + pTy * perp2[1];
            double pz = pLong * nz + pTx * perp1[2] + pTy * perp2[2];
            double e = Math.sqrt(px * px + py * py + pz * pz + hadronMass * hadronMass);

            hadrons.add(new Hadron(hadronPdg, new FourMomentum(e, px, py, pz)));

            remainingMom = remainingMom.subtract(new FourMomentum(e, px, py, pz));
            remainingMass = remainingMom.invariantMass();
        }

        // Last hadron gets remaining momentum
        if (remainingMass > M_PI) {
            hadrons.add(new Hadron(randomPion(), remainingMom));
        } else if (remainingMom.getE() > 0.01) {
            hadrons.add(new Hadron(22, remainingMom)); // photon as catch-all
        }

        return hadrons;
    }

    /**
     * Samples z from the Lund fragmentation function:
     * f(z) = (1/z) · (1-z)^a, normalized on [zMin, zMax].
     * Using rejection sampling.
     */
    private double sampleLundZ() {
        double zMin = 0.01;
        double zMax = 0.99;
        while (true) {
            // Sample from 1/z distribution
            double z = zMin * Math.pow(zMax / zMin, rng.nextDouble());
            // Accept with probability (1-z)^a
            double accept = Math.pow(1.0 - z, LUND_A);
            if (rng.nextDouble() < accept) {
                return z;
            }
        }
    }

    /**
     * Samples a vacuum quark flavor: u(1), d(2), s(3).
     */
    private int sampleVacuumQuark() {
        double r = rng.nextDouble() * QUARK_WEIGHT_SUM;
        if (r < QUARK_WEIGHTS[0]) return 2;  // u
        if (r < QUARK_WEIGHTS[0] + QUARK_WEIGHTS[1]) return 1;  // d
        return 3;  // s
    }

    /**
     * Assigns a meson PDG ID based on a quark flavor.
     */
    private int assignMeson(int quarkFlavor) {
        // Simplified meson assignment
        double r = rng.nextDouble();
        int sign = rng.nextDouble() < 0.5 ? 1 : -1;

        if (quarkFlavor == 3) {
            // Strange mesons
            if (r < 0.7) return sign * 321;  // K±
            return sign * 311;                 // K0 → approximate as K_L (130) handled elsewhere
        }
        // Light mesons
        if (r < 0.75) return sign * 211;  // pi±
        return 111;                         // pi0
    }

    private double mesonMass(int pdgId) {
        int absPdg = Math.abs(pdgId);
        if (absPdg == 211 || absPdg == 111) return M_PI;
        if (absPdg == 321 || absPdg == 311 || absPdg == 130 || absPdg == 310) return M_K;
        if (absPdg == 113) return M_RHO;
        return M_PI; // default
    }

    private int randomPion() {
        double r = rng.nextDouble();
        if (r < 0.33) return 211;   // pi+
        if (r < 0.67) return -211;  // pi-
        return 111;                   // pi0
    }

    private int randomBaryon() {
        return rng.nextDouble() < 0.5 ? 2212 : -2212; // proton/antiproton
    }

    private FourMomentum sumMomenta(List<PartonShower.ShowerParton> partons) {
        FourMomentum sum = FourMomentum.atRest(0);
        for (var p : partons) {
            sum = sum.add(p.momentum());
        }
        return sum;
    }

    private static double[] perpVector(double nx, double ny, double nz) {
        double[] perp;
        if (Math.abs(nx) < 0.9) {
            perp = new double[]{0, -nz, ny};
        } else {
            perp = new double[]{-nz, 0, nx};
        }
        double norm = Math.sqrt(perp[0] * perp[0] + perp[1] * perp[1] + perp[2] * perp[2]);
        if (norm > 0) {
            perp[0] /= norm; perp[1] /= norm; perp[2] /= norm;
        }
        return perp;
    }

    private static double[] cross(double nx, double ny, double nz, double[] v) {
        return new double[]{
                ny * v[2] - nz * v[1],
                nz * v[0] - nx * v[2],
                nx * v[1] - ny * v[0]
        };
    }

    /**
     * Invariant mass of a string system.
     */
    private record StringSystem(FourMomentum momentum) {}
}
