package com.lhcsim.physics.detector.components;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.detector.RecoObject;
import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;

import java.util.List;

public class MissingET {

    private final RngStream rng;

    public MissingET(RngStream rng) {
        this.rng = rng;
    }

    /**
     * Computes MET from the negative vector sum of all reconstructed objects' transverse momenta,
     * smeared by 5 GeV Gaussian + 0.5*sqrt(sum_ET).
     */
    public RecoObject compute(List<RecoObject> allObjects) {
        double sumPx = 0;
        double sumPy = 0;
        double sumET = 0;

        for (RecoObject obj : allObjects) {
            if (obj.type() == ObjectType.MET) continue;
            double px = obj.pt() * Math.cos(obj.phi());
            double py = obj.pt() * Math.sin(obj.phi());
            sumPx += px;
            sumPy += py;
            sumET += obj.pt();
        }

        double metPx = -sumPx;
        double metPy = -sumPy;

        // Smear: 5 GeV Gaussian + 0.5*sqrt(sumET)
        double smearSigma = 5.0 + 0.5 * Math.sqrt(Math.max(sumET, 0));
        metPx += rng.nextGaussian(0, smearSigma);
        metPy += rng.nextGaussian(0, smearSigma);

        double metPt = Math.sqrt(metPx * metPx + metPy * metPy);
        double metPhi = Math.atan2(metPy, metPx);

        return new RecoObject(metPt, 0, metPhi, 0, ObjectType.MET, 0.5, 0);
    }
}
