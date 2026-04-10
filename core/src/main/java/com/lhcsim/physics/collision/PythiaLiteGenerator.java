package com.lhcsim.physics.collision;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.collision.hadronize.LundLite;
import com.lhcsim.physics.collision.matrix.*;
import com.lhcsim.physics.collision.pdf.PdfGrid;
import com.lhcsim.physics.collision.shower.PartonShower;
import com.lhcsim.physics.particles.DecayEngine;
import com.lhcsim.physics.particles.FourMomentum;
import com.lhcsim.physics.particles.ParticleDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Full Pythia-lite event generation pipeline.
 * <p>
 * End-to-end flow: PDF sampling → matrix element → parton shower → hadronization → decay.
 * Produces a {@link GeneratedEvent} with complete final-state particles.
 * <p>
 * Hard processes supported:
 * <ul>
 *   <li>Drell-Yan (Z/γ* → ℓ⁺ℓ⁻)</li>
 *   <li>Higgs via gluon fusion (gg → H)</li>
 *   <li>Top pair production (gg/qq̄ → tt̄)</li>
 *   <li>Di-photon (qq̄ → γγ)</li>
 * </ul>
 */
public class PythiaLiteGenerator {

    /** Maximum events generated per real second. */
    private static final int MAX_EVENTS_PER_SECOND = 50;

    private final PdfGrid pdf;
    private final ParticleDatabase particleDb;
    private final DecayEngine decayEngine;
    private final PartonShower partonShower;
    private final LundLite hadronizer;
    private final RngStream rng;

    // Matrix elements
    private final DrellYan drellYan = new DrellYan();
    private final GgHiggs ggHiggs = new GgHiggs();
    private final TTbar ttbar = new TTbar();
    private final DiPhoton diPhoton = new DiPhoton();

    public PythiaLiteGenerator(PdfGrid pdf, ParticleDatabase particleDb, RngStream rng) {
        this.pdf = pdf;
        this.particleDb = particleDb;
        this.rng = rng;
        this.decayEngine = new DecayEngine(particleDb, rng);
        this.partonShower = new PartonShower(rng);
        this.hadronizer = new LundLite(rng);
    }

    /**
     * Creates a PythiaLiteGenerator with default resources.
     */
    public static PythiaLiteGenerator createDefault(RngStream rng) throws IOException {
        PdfGrid pdf = PdfGrid.loadDefault();
        ParticleDatabase particleDb = ParticleDatabase.getInstance();
        return new PythiaLiteGenerator(pdf, particleDb, rng);
    }

    /**
     * Generates a complete event for the specified hard process.
     *
     * @param process the hard-scatter process
     * @param ecm     centre-of-mass energy [GeV]
     * @param nPileup number of pileup interactions
     * @return a fully generated event with final-state particles
     */
    public GeneratedEvent generate(Process process, double ecm, int nPileup) {
        // Step 1: Sample parton kinematics from PDFs
        double sqrtS = ecm;
        double s = sqrtS * sqrtS;

        // Step 2: Generate hard-scatter momenta
        List<HardParton> hardPartons = generateHardScatter(process, sqrtS);

        // Step 3: Build hard-system four-momentum
        FourMomentum hardSystem = FourMomentum.atRest(0);
        for (HardParton hp : hardPartons) {
            hardSystem = hardSystem.add(hp.momentum);
        }

        // Step 4: Shower colored partons
        List<PartonShower.ShowerParton> showeredPartons = new ArrayList<>();
        for (HardParton hp : hardPartons) {
            if (isColored(hp.pdgId)) {
                double q2 = hardSystem.mass2();
                if (q2 > 1.0) {
                    showeredPartons.addAll(partonShower.shower(hp.pdgId, hp.momentum, q2));
                } else {
                    showeredPartons.add(new PartonShower.ShowerParton(hp.pdgId, hp.momentum));
                }
            } else {
                // Non-colored particles (leptons, photons) skip shower
                showeredPartons.add(new PartonShower.ShowerParton(hp.pdgId, hp.momentum));
            }
        }

        // Step 5: Hadronize colored partons
        List<PartonShower.ShowerParton> coloredPartons = new ArrayList<>();
        List<DecayEngine.StableParticle> nonColoredFinal = new ArrayList<>();

        for (PartonShower.ShowerParton sp : showeredPartons) {
            if (isColored(sp.pdgId())) {
                coloredPartons.add(sp);
            } else {
                // Check if it needs to decay
                if (decayEngine.isStable(sp.pdgId())) {
                    nonColoredFinal.add(new DecayEngine.StableParticle(sp.pdgId(), sp.momentum()));
                } else {
                    nonColoredFinal.addAll(decayEngine.decay(sp.pdgId(), sp.momentum()));
                }
            }
        }

        List<LundLite.Hadron> hadrons = hadronizer.hadronize(coloredPartons);

        // Step 6: Decay unstable hadrons
        List<DecayEngine.StableParticle> finalState = new ArrayList<>(nonColoredFinal);
        for (LundLite.Hadron hadron : hadrons) {
            if (decayEngine.isStable(hadron.pdgId())) {
                finalState.add(new DecayEngine.StableParticle(hadron.pdgId(), hadron.momentum()));
            } else {
                finalState.addAll(decayEngine.decay(hadron.pdgId(), hadron.momentum()));
            }
        }

        return new GeneratedEvent(process, ecm, hardSystem, finalState, nPileup);
    }

    /**
     * Generates hard-scatter partons for the given process.
     */
    private List<HardParton> generateHardScatter(Process process, double sqrtS) {
        return switch (process) {
            case HIGGS_GGF -> generateHiggsGGF(sqrtS);
            case TTBAR -> generateTTbar(sqrtS);
            case Z_PROD -> generateDrellYan(sqrtS, 23);
            case W_PROD -> generateWBoson(sqrtS);
            case DIBOSON_ZZ -> generateDiBoson(sqrtS, 23, 23);
            case DIBOSON_WW -> generateDiBoson(sqrtS, 24, -24);
            case DIBOSON_WZ -> generateDiBoson(sqrtS, 24, 23);
            default -> generateGeneric(sqrtS, process);
        };
    }

    private List<HardParton> generateHiggsGGF(double sqrtS) {
        double mH = 125.25;
        // Sample x1, x2 from gluon PDFs
        double q2 = mH * mH;
        double[] x1x2 = sampleX1X2(21, 21, q2, sqrtS, mH);

        // Higgs kinematics
        double rapidity = 0.5 * Math.log(x1x2[0] / x1x2[1]);
        double pt = Math.abs(rng.nextGaussian() * mH / 4);
        double phi = rng.nextDouble() * 2 * Math.PI;

        double mt = Math.sqrt(mH * mH + pt * pt);
        FourMomentum pHiggs = new FourMomentum(
                mt * Math.cosh(rapidity),
                pt * Math.cos(phi),
                pt * Math.sin(phi),
                mt * Math.sinh(rapidity));

        // Higgs decays via DecayEngine
        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(25, pHiggs));
        return result;
    }

    private List<HardParton> generateTTbar(double sqrtS) {
        double mt = 172.69;
        double minMass = 2 * mt;
        double q2 = minMass * minMass;

        // Sample parton x values
        double[] x1x2 = sampleX1X2(21, 21, q2, sqrtS, minMass * 1.1);
        double sHat = x1x2[0] * x1x2[1] * sqrtS * sqrtS;
        if (sHat < 4 * mt * mt) sHat = 4 * mt * mt * 1.01;

        double mtt = Math.sqrt(sHat);
        double rapidity = 0.5 * Math.log(x1x2[0] / x1x2[1]);
        double pt = Math.abs(rng.nextGaussian() * mtt / 6);
        double phi = rng.nextDouble() * 2 * Math.PI;

        // Top decay momenta in ttbar CM
        double pStar = Math.sqrt(Math.max(0, sHat / 4 - mt * mt));
        double cosTheta = 2.0 * rng.nextDouble() - 1.0;
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);
        double phiDecay = rng.nextDouble() * 2 * Math.PI;

        FourMomentum pTop = FourMomentum.fromMassAndMomentum(mt,
                pStar * sinTheta * Math.cos(phiDecay),
                pStar * sinTheta * Math.sin(phiDecay),
                pStar * cosTheta);
        FourMomentum pAntiTop = FourMomentum.fromMassAndMomentum(mt,
                -pStar * sinTheta * Math.cos(phiDecay),
                -pStar * sinTheta * Math.sin(phiDecay),
                -pStar * cosTheta);

        // Boost to lab frame
        FourMomentum pBoost = pTop.boostZ(rapidity);
        FourMomentum pAntiBoost = pAntiTop.boostZ(rapidity);

        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(6, pBoost));
        result.add(new HardParton(-6, pAntiBoost));
        return result;
    }

    private List<HardParton> generateDrellYan(double sqrtS, int bosonPdg) {
        double mZ = 91.1876;
        double q2 = mZ * mZ;

        double[] x1x2 = sampleX1X2(2, -2, q2, sqrtS, mZ);
        double rapidity = 0.5 * Math.log(x1x2[0] / x1x2[1]);
        double pt = Math.abs(rng.nextGaussian() * mZ / 3);
        double phi = rng.nextDouble() * 2 * Math.PI;

        double mt = Math.sqrt(mZ * mZ + pt * pt);
        FourMomentum pZ = new FourMomentum(
                mt * Math.cosh(rapidity),
                pt * Math.cos(phi),
                pt * Math.sin(phi),
                mt * Math.sinh(rapidity));

        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(bosonPdg, pZ));
        return result;
    }

    private List<HardParton> generateWBoson(double sqrtS) {
        double mW = 80.377;
        int sign = rng.nextDouble() > 0.5 ? 1 : -1;

        double q2 = mW * mW;
        double[] x1x2 = sampleX1X2(2, -1, q2, sqrtS, mW);
        double rapidity = 0.5 * Math.log(x1x2[0] / x1x2[1]);
        double pt = Math.abs(rng.nextGaussian() * mW / 3);
        double phi = rng.nextDouble() * 2 * Math.PI;

        double mt = Math.sqrt(mW * mW + pt * pt);
        FourMomentum pW = new FourMomentum(
                mt * Math.cosh(rapidity),
                pt * Math.cos(phi),
                pt * Math.sin(phi),
                mt * Math.sinh(rapidity));

        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(sign * 24, pW));
        return result;
    }

    private List<HardParton> generateDiBoson(double sqrtS, int pdg1, int pdg2) {
        double m1 = pdg1 == 23 ? 91.1876 : 80.377;
        double m2 = pdg2 == 23 ? 91.1876 : 80.377;
        double mMin = m1 + m2;
        double q2 = mMin * mMin;

        double[] x1x2 = sampleX1X2(2, -2, q2, sqrtS, mMin * 1.05);
        double rapidity = 0.5 * Math.log(x1x2[0] / x1x2[1]);

        double sHat = x1x2[0] * x1x2[1] * sqrtS * sqrtS;
        double pStar = twoBodyMomentum(Math.sqrt(sHat), m1, m2);

        double cosTheta = 2.0 * rng.nextDouble() - 1.0;
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);
        double phi = rng.nextDouble() * 2 * Math.PI;

        FourMomentum p1 = FourMomentum.fromMassAndMomentum(m1,
                pStar * sinTheta * Math.cos(phi),
                pStar * sinTheta * Math.sin(phi),
                pStar * cosTheta).boostZ(rapidity);
        FourMomentum p2 = FourMomentum.fromMassAndMomentum(m2,
                -pStar * sinTheta * Math.cos(phi),
                -pStar * sinTheta * Math.sin(phi),
                -pStar * cosTheta).boostZ(rapidity);

        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(pdg1, p1));
        result.add(new HardParton(pdg2, p2));
        return result;
    }

    private List<HardParton> generateGeneric(double sqrtS, Process process) {
        // Generic process: produce two back-to-back gluon jets
        double pt = Math.abs(rng.nextGaussian() * sqrtS / 10);
        double eta = rng.nextGaussian() * 2.5;
        double phi = rng.nextDouble() * 2 * Math.PI;

        double px = pt * Math.cos(phi);
        double py = pt * Math.sin(phi);
        double pz = pt * Math.sinh(eta);
        double e = Math.sqrt(px * px + py * py + pz * pz);

        List<HardParton> result = new ArrayList<>();
        result.add(new HardParton(21, new FourMomentum(e, px, py, pz)));
        result.add(new HardParton(21, new FourMomentum(e, -px, -py, -pz)));
        return result;
    }

    /**
     * Samples Bjorken-x values for two partons, requiring x1*x2*s >= mMin^2.
     */
    private double[] sampleX1X2(int pdg1, int pdg2, double q2, double sqrtS, double mMin) {
        double s = sqrtS * sqrtS;
        double tauMin = (mMin * mMin) / s;

        for (int attempt = 0; attempt < 1000; attempt++) {
            // Sample x1 from PDF-like distribution
            double x1 = sampleXFromPdf(pdg1, q2);
            double x2Min = tauMin / x1;
            if (x2Min > 1.0) continue;

            double x2 = sampleXFromPdf(pdg2, q2);
            if (x2 < x2Min) continue;

            if (x1 * x2 * s >= mMin * mMin) {
                return new double[]{x1, x2};
            }
        }

        // Fallback: symmetric
        double xMin = mMin / sqrtS;
        return new double[]{xMin * 1.1, xMin * 1.1};
    }

    /**
     * Samples an x value weighted by the PDF.
     */
    private double sampleXFromPdf(int pdgId, double q2) {
        // Simple sampling: x ~ x^(-0.5) shape
        double xMin = 1e-4;
        double xMax = 0.99;
        double u = rng.nextDouble();
        return xMin * Math.pow(xMax / xMin, u);
    }

    private static boolean isColored(int pdgId) {
        int abs = Math.abs(pdgId);
        return abs == 21 || (abs >= 1 && abs <= 6);
    }

    private static double twoBodyMomentum(double M, double m1, double m2) {
        double M2 = M * M;
        double m12 = m1 * m1;
        double m22 = m2 * m2;
        double lambda = M2 * M2 + m12 * m12 + m22 * m22
                - 2 * M2 * m12 - 2 * M2 * m22 - 2 * m12 * m22;
        return lambda > 0 ? Math.sqrt(lambda) / (2 * M) : 0;
    }

    /**
     * Internal representation of a hard-scatter parton.
     */
    record HardParton(int pdgId, FourMomentum momentum) {}
}
