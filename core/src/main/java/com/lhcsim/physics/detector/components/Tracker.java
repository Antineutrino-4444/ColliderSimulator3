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

public class Tracker {
    private final double bField;
    private final double ptMin;
    private final double resA;
    private final double resB;
    private final double etaMax;
    private final double trackerRadius; // meters
    private final RngStream rng;

    public Tracker(double bField, double ptMin, double resA, double resB, double etaMax, double trackerRadius, RngStream rng) {
        this.bField = bField;
        this.ptMin = ptMin;
        this.resA = resA;
        this.resB = resB;
        this.etaMax = etaMax;
        this.trackerRadius = trackerRadius;
        this.rng = rng;
    }

    public record TrackerResult(List<RecoObject> recoObjects, List<RawHit> hits, List<List<double[]>> helixPolylines) {}

    public TrackerResult simulate(List<DecayEngine.StableParticle> particles) {
        List<RecoObject> recoObjects = new ArrayList<>();
        List<RawHit> hits = new ArrayList<>();
        List<List<double[]>> polylines = new ArrayList<>();

        for (DecayEngine.StableParticle sp : particles) {
            FourMomentum p = sp.momentum();
            int pdgId = sp.pdgId();
            int absPdg = Math.abs(pdgId);

            int charge = getCharge(pdgId);
            if (charge == 0) continue;

            double pt = p.pT();
            if (pt < ptMin) continue;

            double eta = p.eta();
            if (Math.abs(eta) > etaMax) continue;

            // Smear pT: sigma/pT = a + b*pT
            double sigma = pt * (resA + resB * pt);
            double smearedPt = pt + rng.nextGaussian(0, sigma);
            smearedPt = Math.max(smearedPt, 0.01);

            ObjectType type = classifyCharged(absPdg);
            double mass = getMass(absPdg);

            recoObjects.add(new RecoObject(smearedPt, eta, p.phi(), mass, type, 0.9, charge));

            Helix helix = Helix.from(p, charge, bField);
            polylines.add(helix.polyline(trackerRadius, 5.0));

            hits.add(new RawHit(DetectorComponent.TRACKER, eta, p.phi(), trackerRadius, pt, pdgId));
        }

        return new TrackerResult(recoObjects, hits, polylines);
    }

    private static int getCharge(int pdgId) {
        int absPdg = Math.abs(pdgId);
        int sign = pdgId > 0 ? 1 : -1;
        return switch (absPdg) {
            case 11 -> -sign;  // electron: pdg=11 has charge -1
            case 13 -> -sign;  // muon
            case 211 -> sign;  // pi+
            case 321 -> sign;  // K+
            case 2212 -> sign; // proton
            default -> 0;
        };
    }

    private static ObjectType classifyCharged(int absPdg) {
        return switch (absPdg) {
            case 11 -> ObjectType.ELECTRON;
            case 13 -> ObjectType.MUON;
            default -> ObjectType.JET;
        };
    }

    private static double getMass(int absPdg) {
        return switch (absPdg) {
            case 11 -> 0.000511;
            case 13 -> 0.10566;
            case 211 -> 0.13957;
            case 321 -> 0.49368;
            case 2212 -> 0.93827;
            default -> 0.13957;
        };
    }
}
