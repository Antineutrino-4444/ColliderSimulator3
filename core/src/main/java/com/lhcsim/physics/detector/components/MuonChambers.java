package com.lhcsim.physics.detector.components;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.detector.DetectorComponent;
import com.lhcsim.physics.detector.RawHit;
import com.lhcsim.physics.detector.RecoObject;
import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;

import java.util.ArrayList;
import java.util.List;

public class MuonChambers {

    private final double resA;
    private final double resB;
    private final double etaMax;
    private final double innerRadius;
    private final double outerRadius;
    private final RngStream rng;

    public MuonChambers(double resA, double resB, double etaMax, double innerRadius, double outerRadius, RngStream rng) {
        this.resA = resA;
        this.resB = resB;
        this.etaMax = etaMax;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.rng = rng;
    }

    public record MuonResult(List<RecoObject> combined, List<RawHit> hits) {}

    /**
     * Takes tracker muon candidates and applies muon chamber measurement.
     * Combines with tracker measurement using inverse-variance weighting.
     *
     * @param trackerMuons muons as reconstructed by the tracker
     * @param trackerResA  tracker resolution parameter a
     * @param trackerResB  tracker resolution parameter b
     */
    public MuonResult simulate(List<RecoObject> trackerMuons, double trackerResA, double trackerResB) {
        List<RecoObject> combined = new ArrayList<>();
        List<RawHit> hits = new ArrayList<>();

        for (RecoObject trackerMuon : trackerMuons) {
            if (trackerMuon.type() != ObjectType.MUON) continue;
            if (Math.abs(trackerMuon.eta()) > etaMax) continue;

            double trackerPt = trackerMuon.pt();

            // Muon chamber independent measurement (worse resolution)
            double muonSigma = trackerPt * (resA + resB * trackerPt);
            double muonPt = trackerPt + rng.nextGaussian(0, muonSigma);
            muonPt = Math.max(muonPt, 0.01);

            // Inverse-variance weighted combination
            double trackerSigma = trackerPt * (trackerResA + trackerResB * trackerPt);
            double w1 = 1.0 / (trackerSigma * trackerSigma);
            double w2 = 1.0 / (muonSigma * muonSigma);
            double combinedPt = (w1 * trackerPt + w2 * muonPt) / (w1 + w2);

            combined.add(new RecoObject(combinedPt, trackerMuon.eta(), trackerMuon.phi(),
                    0.10566, ObjectType.MUON, 0.95, trackerMuon.charge()));

            hits.add(new RawHit(DetectorComponent.MUON_CHAMBERS, trackerMuon.eta(), trackerMuon.phi(),
                    (innerRadius + outerRadius) / 2, combinedPt, 13));
        }

        return new MuonResult(combined, hits);
    }
}
