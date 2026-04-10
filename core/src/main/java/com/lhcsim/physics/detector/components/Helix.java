package com.lhcsim.physics.detector.components;

import com.lhcsim.physics.particles.FourMomentum;

import java.util.ArrayList;
import java.util.List;

public final class Helix {
    private final double radius;      // meters
    private final double phiStart;
    private final double pitchPerRad; // meters of z advance per radian
    private final int chargeSign;

    private Helix(double radius, double phiStart, double pitchPerRad, int chargeSign) {
        this.radius = radius;
        this.phiStart = phiStart;
        this.pitchPerRad = pitchPerRad;
        this.chargeSign = chargeSign;
    }

    public static Helix from(FourMomentum p, int charge, double bTesla) {
        double pT = p.pT();
        if (pT < 1e-6 || charge == 0 || Math.abs(bTesla) < 1e-6) {
            return new Helix(Double.MAX_VALUE, Math.atan2(p.getPy(), p.getPx()), 0, 0);
        }
        double r = pT / (0.3 * Math.abs(bTesla)); // radius in meters
        double phi0 = Math.atan2(p.getPy(), p.getPx());
        double pz = p.getPz();
        double pitchPerRad = pz / pT * r;
        int sign = charge > 0 ? -1 : 1;
        return new Helix(r, phi0, pitchPerRad, sign);
    }

    public double radius() { return radius; }

    /**
     * Generate a polyline sampling the helix from the origin.
     * @param maxRadius maximum transverse distance from origin in meters
     * @param stepCm step size in centimeters
     * @return list of [x, y, z] points in meters
     */
    public List<double[]> polyline(double maxRadius, double stepCm) {
        List<double[]> points = new ArrayList<>();
        double stepM = stepCm / 100.0;

        if (radius >= 1e10 || chargeSign == 0) {
            double cos = Math.cos(phiStart);
            double sin = Math.sin(phiStart);
            double totalPath = maxRadius;
            int nSteps = Math.max(2, (int) (totalPath / stepM));
            for (int i = 0; i <= nSteps; i++) {
                double s = totalPath * i / nSteps;
                points.add(new double[]{s * cos, s * sin, 0});
            }
            return points;
        }

        double maxAngle = 2 * Math.PI;
        double angleStep = stepM / radius;

        points.add(new double[]{0, 0, 0});

        for (double t = angleStep; t <= maxAngle; t += angleStep) {
            double angle = phiStart + chargeSign * t;
            double x = radius * (Math.sin(angle) - Math.sin(phiStart));
            double y = radius * (-Math.cos(angle) + Math.cos(phiStart));
            double z = pitchPerRad * t;

            double rTransverse = Math.sqrt(x * x + y * y);
            if (rTransverse > maxRadius) break;

            points.add(new double[]{x, y, z});
        }

        return points;
    }
}
