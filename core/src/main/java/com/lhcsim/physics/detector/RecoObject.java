package com.lhcsim.physics.detector;

import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;

public record RecoObject(double pt, double eta, double phi, double mass, ObjectType type, double quality, int charge) {

    public double energy() {
        double pz = pt * Math.sinh(eta);
        double p2 = pt * pt + pz * pz;
        return Math.sqrt(p2 + mass * mass);
    }

    public double px() { return pt * Math.cos(phi); }

    public double py() { return pt * Math.sin(phi); }

    public double pz() { return pt * Math.sinh(eta); }
}
