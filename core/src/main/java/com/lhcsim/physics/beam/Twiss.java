package com.lhcsim.physics.beam;

/**
 * Courant-Snyder (Twiss) beam-envelope parameters as an immutable record.
 * <p>
 * Describes the shape and orientation of the beam ellipse in both
 * transverse planes, together with the dispersion function.
 * The three Twiss parameters per plane satisfy beta * gamma - alpha^2 = 1.
 * <p>
 * Reference: E. D. Courant and H. S. Snyder, "Theory of the Alternating-Gradient
 * Synchrotron," Annals of Physics 3, 1-48 (1958).
 *
 * @param betaX  horizontal beta function [m]
 * @param alphaX horizontal alpha (-1/2 * d(beta)/ds)
 * @param gammaX horizontal gamma = (1 + alpha^2) / beta [1/m]
 * @param betaY  vertical beta function [m]
 * @param alphaY vertical alpha
 * @param gammaY vertical gamma [1/m]
 * @param dx     horizontal dispersion D_x [m]
 * @param dpx    horizontal dispersion slope D'_x
 * @param dy     vertical dispersion D_y [m]
 * @param dpy    vertical dispersion slope D'_y
 */
public record Twiss(
        double betaX, double alphaX, double gammaX,
        double betaY, double alphaY, double gammaY,
        double dx, double dpx, double dy, double dpy
) {

    /**
     * Convenience factory that computes gamma from beta and alpha.
     *
     * @param betaX  horizontal beta [m]
     * @param alphaX horizontal alpha
     * @param betaY  vertical beta [m]
     * @param alphaY vertical alpha
     * @param dx     horizontal dispersion [m]
     * @param dpx    horizontal dispersion slope
     * @return a Twiss record with gamma computed from the other parameters
     */
    public static Twiss of(double betaX, double alphaX,
                           double betaY, double alphaY,
                           double dx, double dpx) {
        double gx = (1.0 + alphaX * alphaX) / betaX;
        double gy = (1.0 + alphaY * alphaY) / betaY;
        return new Twiss(betaX, alphaX, gx, betaY, alphaY, gy, dx, dpx, 0.0, 0.0);
    }

    /**
     * Converts to the mutable legacy {@link TwissParameters}.
     */
    public TwissParameters toTwissParameters() {
        return new TwissParameters(betaX, alphaX, betaY, alphaY, dx, dpx);
    }

    /**
     * Creates a Twiss from the legacy {@link TwissParameters}.
     */
    public static Twiss fromTwissParameters(TwissParameters tp) {
        return of(tp.getBetaX(), tp.getAlphaX(),
                tp.getBetaY(), tp.getAlphaY(),
                tp.getDispX(), tp.getDispPX());
    }
}
