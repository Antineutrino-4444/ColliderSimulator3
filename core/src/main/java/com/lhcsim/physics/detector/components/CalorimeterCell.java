package com.lhcsim.physics.detector.components;

import com.lhcsim.physics.detector.DetectorComponent;

public record CalorimeterCell(double eta, double phi, double energyDeposited, DetectorComponent type) {}
