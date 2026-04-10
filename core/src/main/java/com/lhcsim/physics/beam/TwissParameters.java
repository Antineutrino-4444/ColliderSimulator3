package com.lhcsim.physics.beam;

/**
 * Courant–Snyder (Twiss) beam-envelope parameters.
 * <p>
 * These describe the shape and orientation of the beam ellipse in both
 * transverse planes, together with the dispersion function and its slope.
 * The three Twiss parameters per plane satisfy the relation β·γ − α² = 1.
 */
public class TwissParameters {

    private double betaX;
    private double alphaX;
    private double betaY;
    private double alphaY;
    private double dispX;
    private double dispPX;

    /** Default constructor – all parameters initialised to zero. */
    public TwissParameters() {
    }

    /**
     * Full constructor.
     *
     * @param betaX  horizontal beta function [m]
     * @param alphaX horizontal alpha (−½ dβ/ds)
     * @param betaY  vertical beta function [m]
     * @param alphaY vertical alpha
     * @param dispX  horizontal dispersion D_x [m]
     * @param dispPX horizontal dispersion slope D'_x
     */
    public TwissParameters(double betaX, double alphaX,
                           double betaY, double alphaY,
                           double dispX, double dispPX) {
        this.betaX = betaX;
        this.alphaX = alphaX;
        this.betaY = betaY;
        this.alphaY = alphaY;
        this.dispX = dispX;
        this.dispPX = dispPX;
    }

    // ── Derived quantities ──────────────────────────────────────────

    /** Horizontal Twiss γ_x = (1 + α_x²) / β_x. */
    public double gammaX() {
        return (1.0 + alphaX * alphaX) / betaX;
    }

    /** Vertical Twiss γ_y = (1 + α_y²) / β_y. */
    public double gammaY() {
        return (1.0 + alphaY * alphaY) / betaY;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public double getBetaX() {
        return betaX;
    }

    public void setBetaX(double betaX) {
        this.betaX = betaX;
    }

    public double getAlphaX() {
        return alphaX;
    }

    public void setAlphaX(double alphaX) {
        this.alphaX = alphaX;
    }

    public double getBetaY() {
        return betaY;
    }

    public void setBetaY(double betaY) {
        this.betaY = betaY;
    }

    public double getAlphaY() {
        return alphaY;
    }

    public void setAlphaY(double alphaY) {
        this.alphaY = alphaY;
    }

    public double getDispX() {
        return dispX;
    }

    public void setDispX(double dispX) {
        this.dispX = dispX;
    }

    public double getDispPX() {
        return dispPX;
    }

    public void setDispPX(double dispPX) {
        this.dispPX = dispPX;
    }

    @Override
    public String toString() {
        return String.format(
                "TwissParameters(βx=%.4f, αx=%.4f, γx=%.4f, βy=%.4f, αy=%.4f, γy=%.4f, Dx=%.4f, D'x=%.4f)",
                betaX, alphaX, gammaX(), betaY, alphaY, gammaY(), dispX, dispPX);
    }
}
