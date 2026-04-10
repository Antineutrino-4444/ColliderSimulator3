package com.lhcsim.physics.detector.components;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.detector.DetectorComponent;
import com.lhcsim.physics.detector.RawHit;
import com.lhcsim.physics.detector.RecoObject;
import com.lhcsim.physics.particles.DecayEngine;
import com.lhcsim.physics.particles.FourMomentum;
import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;

import java.util.ArrayList;
import java.util.List;

public class HCAL {

    private static final double CELL_DELTA_ETA = 0.087;
    private static final double CELL_DELTA_PHI = 0.087;

    private final double resA;
    private final double resB;
    private final double innerRadius;
    private final double outerRadius;
    private final double etaMax;
    private final RngStream rng;

    public HCAL(double resA, double resB, double innerRadius, double outerRadius, double etaMax, RngStream rng) {
        this.resA = resA;
        this.resB = resB;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.etaMax = etaMax;
        this.rng = rng;
    }

    public record HcalResult(List<RecoObject> recoObjects, List<CalorimeterCell> cells, List<RawHit> hits) {}

    /**
     * Simulates the HCAL response for hadrons.
     * Accepts: charged hadrons (pi±, K±, protons) and neutral hadrons.
     */
    public HcalResult simulate(List<DecayEngine.StableParticle> particles) {
        List<RecoObject> recoObjects = new ArrayList<>();
        List<CalorimeterCell> cells = new ArrayList<>();
        List<RawHit> hits = new ArrayList<>();

        for (DecayEngine.StableParticle sp : particles) {
            int absPdg = Math.abs(sp.pdgId());
            if (!isHadron(absPdg)) continue;

            FourMomentum p = sp.momentum();
            double eta = p.eta();
            if (Math.abs(eta) > etaMax) continue;

            double energy = p.getE();
            if (energy < 0.5) continue;

            double sigma = energy * resolution(energy);
            double smearedE = energy + rng.nextGaussian(0, sigma);
            smearedE = Math.max(smearedE, 0.01);

            double phi = p.phi();
            double pt = p.pT();
            double smearedPt = pt * (smearedE / energy);

            double mass = getMass(absPdg);
            int charge = getCharge(sp.pdgId());

            recoObjects.add(new RecoObject(smearedPt, eta, phi, mass, ObjectType.JET, 0.8, charge));

            depositInCells(eta, phi, smearedE, cells);
            hits.add(new RawHit(DetectorComponent.HCAL, eta, phi, (innerRadius + outerRadius) / 2, smearedE, sp.pdgId()));
        }

        return new HcalResult(recoObjects, cells, hits);
    }

    private void depositInCells(double eta, double phi, double energy, List<CalorimeterCell> cells) {
        double cellEta = snapToGrid(eta, CELL_DELTA_ETA);
        double cellPhi = snapToGrid(phi, CELL_DELTA_PHI);

        double primaryFraction = 0.6 + rng.nextDouble() * 0.2;
        cells.add(new CalorimeterCell(cellEta, cellPhi, energy * primaryFraction, DetectorComponent.HCAL));

        double remaining = energy * (1 - primaryFraction);
        int nNeighbors = 1 + (int) (rng.nextDouble() * 3);
        double spreadSigma = 0.04;
        for (int i = 0; i < nNeighbors; i++) {
            double dEta = rng.nextGaussian(0, spreadSigma);
            double dPhi = rng.nextGaussian(0, spreadSigma);
            double neighborEta = snapToGrid(eta + dEta, CELL_DELTA_ETA);
            double neighborPhi = snapToGrid(phi + dPhi, CELL_DELTA_PHI);
            cells.add(new CalorimeterCell(neighborEta, neighborPhi, remaining / nNeighbors, DetectorComponent.HCAL));
        }
    }

    private double resolution(double energy) {
        if (energy <= 0) return 0;
        return Math.sqrt(resA * resA / energy + resB * resB);
    }

    private static boolean isHadron(int absPdg) {
        return absPdg == 211 || absPdg == 321 || absPdg == 2212 || absPdg == 2112 || absPdg == 130;
    }

    private static double getMass(int absPdg) {
        return switch (absPdg) {
            case 211 -> 0.13957;
            case 321 -> 0.49368;
            case 2212 -> 0.93827;
            case 2112 -> 0.93957;
            case 130 -> 0.49761;
            default -> 0.13957;
        };
    }

    private static int getCharge(int pdgId) {
        int absPdg = Math.abs(pdgId);
        int sign = pdgId > 0 ? 1 : -1;
        return switch (absPdg) {
            case 211 -> sign;
            case 321 -> sign;
            case 2212 -> sign;
            default -> 0;
        };
    }

    private static double snapToGrid(double val, double cellSize) {
        return Math.floor(val / cellSize) * cellSize + cellSize / 2;
    }
}
