package com.lhcsim.physics.detector;

public record RawHit(DetectorComponent component, double eta, double phi, double r, double energy, int pdgId) {}
