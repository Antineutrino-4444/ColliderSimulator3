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

public class ECAL {

    private static final double CELL_DELTA_ETA = 0.025;
    private static final double CELL_DELTA_PHI = 0.025;

    private final double resA;
    private final double resB;
    private final double innerRadius;
    private final double outerRadius;
    private final double etaMax;
    private final RngStream rng;

    public ECAL(double resA, double resB, double innerRadius, double outerRadius, double etaMax, RngStream rng) {
        this.resA = resA;
        this.resB = resB;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.etaMax = etaMax;
        this.rng = rng;
    }

    public record EcalResult(List<RecoObject> recoObjects, List<CalorimeterCell> cells, List<RawHit> hits) {}

    public EcalResult simulate(List<DecayEngine.StableParticle> particles) {
        List<RecoObject> recoObjects = new ArrayList<>();
        List<CalorimeterCell> cells = new ArrayList<>();
        List<RawHit> hits = new ArrayList<>();

        for (DecayEngine.StableParticle sp : particles) {
            int absPdg = Math.abs(sp.pdgId());
            if (absPdg != 22 && absPdg != 11) continue;

            FourMomentum p = sp.momentum();
            double eta = p.eta();
            if (Math.abs(eta) > etaMax) continue;

            double energy = p.getE();
            if (energy < 0.1) continue;

            double sigma = energy * resolution(energy);
            double smearedE = energy + rng.nextGaussian(0, sigma);
            smearedE = Math.max(smearedE, 0.01);

            double phi = p.phi();
            double pt = p.pT();
            double smearedPt = pt * (smearedE / energy);

            ObjectType type = absPdg == 22 ? ObjectType.PHOTON : ObjectType.ELECTRON;
            double mass = absPdg == 22 ? 0.0 : 0.000511;
            int charge = absPdg == 22 ? 0 : (sp.pdgId() > 0 ? -1 : 1);

            recoObjects.add(new RecoObject(smearedPt, eta, phi, mass, type, 0.9, charge));

            depositInCells(eta, phi, smearedE, cells);

            hits.add(new RawHit(DetectorComponent.ECAL, eta, phi, (innerRadius + outerRadius) / 2, smearedE, sp.pdgId()));
        }

        return new EcalResult(recoObjects, cells, hits);
    }

    private void depositInCells(double eta, double phi, double energy, List<CalorimeterCell> cells) {
        double cellEta = snapToGrid(eta, CELL_DELTA_ETA);
        double cellPhi = snapToGrid(phi, CELL_DELTA_PHI);

        double spreadSigma = 0.01;
        double primaryFraction = 0.7 + rng.nextDouble() * 0.15;
        cells.add(new CalorimeterCell(cellEta, cellPhi, energy * primaryFraction, DetectorComponent.ECAL));

        double remaining = energy * (1 - primaryFraction);
        int nNeighbors = 1 + (int) (rng.nextDouble() * 3);
        for (int i = 0; i < nNeighbors; i++) {
            double dEta = rng.nextGaussian(0, spreadSigma);
            double dPhi = rng.nextGaussian(0, spreadSigma);
            double neighborEta = snapToGrid(eta + dEta, CELL_DELTA_ETA);
            double neighborPhi = snapToGrid(phi + dPhi, CELL_DELTA_PHI);
            cells.add(new CalorimeterCell(neighborEta, neighborPhi, remaining / nNeighbors, DetectorComponent.ECAL));
        }
    }

    private double resolution(double energy) {
        if (energy <= 0) return 0;
        return Math.sqrt(resA * resA / energy + resB * resB);
    }

    private static double snapToGrid(double val, double cellSize) {
        return Math.floor(val / cellSize) * cellSize + cellSize / 2;
    }
}
