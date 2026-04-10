package com.lhcsim.physics.beam;

import com.lhcsim.physics.accelerator.AcceleratorElement;
import com.lhcsim.physics.accelerator.Lattice;

import java.util.ArrayList;
import java.util.List;

/**
 * Propagates Courant-Snyder (Twiss) parameters through a lattice.
 * <p>
 * Computes periodic Twiss parameters from the one-turn matrix, and
 * propagates them element-by-element through any lattice.
 * <p>
 * Reference: H. Wiedemann, "Particle Accelerator Physics", 4th ed., Springer (2015),
 * Chapter 8: Periodic Focusing Systems.
 */
public final class TwissPropagator {

    private TwissPropagator() {}

    /**
     * Computes the periodic (matched) Twiss parameters at the entrance
     * of a closed lattice from its one-turn matrix.
     * <p>
     * Uses the standard extraction:
     * <pre>
     *   cos(2*pi*Q) = (M11 + M22) / 2
     *   beta = M12 / sin(2*pi*Q)
     *   alpha = (M11 - M22) / (2 * sin(2*pi*Q))
     * </pre>
     * Applied independently to horizontal (0,1) and vertical (2,3) 2x2 blocks.
     *
     * @param lattice the closed lattice
     * @param gamma   Lorentz gamma of the reference particle
     * @return the periodic Twiss parameters at s=0
     * @throws IllegalStateException if the lattice is unstable
     */
    public static Twiss computePeriodic(Lattice lattice, double gamma) {
        TransferMatrix m = lattice.computeOneTurnMatrix(gamma);

        // Horizontal plane
        double traceX = m.get(0, 0) + m.get(1, 1);
        if (Math.abs(traceX) >= 2.0) {
            throw new IllegalStateException(
                    "Lattice is unstable in horizontal plane: |trace| = " + Math.abs(traceX));
        }
        double cosPhiX = traceX / 2.0;
        double sinPhiX = Math.sqrt(1.0 - cosPhiX * cosPhiX);
        if (m.get(0, 1) < 0) {
            sinPhiX = -sinPhiX;
        }
        double betaX = m.get(0, 1) / sinPhiX;
        double alphaX = (m.get(0, 0) - m.get(1, 1)) / (2.0 * sinPhiX);
        double gammaXp = (1.0 + alphaX * alphaX) / betaX;

        // Vertical plane
        double traceY = m.get(2, 2) + m.get(3, 3);
        if (Math.abs(traceY) >= 2.0) {
            throw new IllegalStateException(
                    "Lattice is unstable in vertical plane: |trace| = " + Math.abs(traceY));
        }
        double cosPhiY = traceY / 2.0;
        double sinPhiY = Math.sqrt(1.0 - cosPhiY * cosPhiY);
        if (m.get(2, 3) < 0) {
            sinPhiY = -sinPhiY;
        }
        double betaY = m.get(2, 3) / sinPhiY;
        double alphaY = (m.get(2, 2) - m.get(3, 3)) / (2.0 * sinPhiY);
        double gammaYp = (1.0 + alphaY * alphaY) / betaY;

        // Dispersion: solve (I - M_1x1) * [Dx, D'x]^T = [M16, M26]^T
        // from the 2×2 system
        double a11 = 1.0 - m.get(0, 0);
        double a12 = -m.get(0, 1);
        double a21 = -m.get(1, 0);
        double a22 = 1.0 - m.get(1, 1);
        double b1 = m.get(0, 5);
        double b2 = m.get(1, 5);
        double detA = a11 * a22 - a12 * a21;
        double dx = 0, dpx = 0;
        if (Math.abs(detA) > 1e-15) {
            dx = (a22 * b1 - a12 * b2) / detA;
            dpx = (-a21 * b1 + a11 * b2) / detA;
        }

        return new Twiss(betaX, alphaX, gammaXp, betaY, alphaY, gammaYp, dx, dpx, 0.0, 0.0);
    }

    /**
     * Propagates Twiss parameters through a single transfer matrix.
     *
     * @param start the input Twiss parameters
     * @param m     the transfer matrix
     * @return the Twiss parameters after transport
     */
    public static Twiss propagate(Twiss start, TransferMatrix m) {
        TwissParameters tp = start.toTwissParameters();
        TwissParameters result = m.transportTwiss(tp);
        return Twiss.fromTwissParameters(result);
    }

    /**
     * Computes the beta function at every element boundary in the lattice.
     *
     * @param lattice the lattice
     * @param gamma   Lorentz gamma of the reference particle
     * @return a list of sample points with s, betaX, and betaY
     */
    public static List<SamplePoint> betaFunction(Lattice lattice, double gamma) {
        Twiss initial = computePeriodic(lattice, gamma);
        List<SamplePoint> result = new ArrayList<>();
        result.add(new SamplePoint(0.0, initial.betaX(), initial.betaY()));

        Twiss current = initial;
        for (AcceleratorElement el : lattice.getElements()) {
            if (el.isActive()) {
                TransferMatrix tm = el.getTransferMatrix(gamma);
                current = propagate(current, tm);
            }
            double sExit = el.getSPosition() + el.getLength();
            result.add(new SamplePoint(sExit, current.betaX(), current.betaY()));
        }

        return result;
    }

    /**
     * A sampled point of the beta function along the lattice.
     *
     * @param s     longitudinal position [m]
     * @param betaX horizontal beta [m]
     * @param betaY vertical beta [m]
     */
    public record SamplePoint(double s, double betaX, double betaY) {}
}
