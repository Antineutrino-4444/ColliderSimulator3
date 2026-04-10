package com.lhcsim.physics.collision;

import com.lhcsim.physics.Constants;
import com.lhcsim.physics.beam.Beam;
import com.lhcsim.physics.beam.Bunch;

/**
 * Static utility methods for luminosity and pileup calculations
 * at a proton–proton interaction point.
 */
public final class LuminosityCalculator {

    /** Speed of light [m/s]. */
    private static final double C_LIGHT = Constants.c;

    private LuminosityCalculator() {
    }

    /**
     * Instantaneous luminosity from two beams at an interaction point.
     * <p>
     * L = (N1 * N2 * f_rev * n_b) / (4pi * sigma_x * sigma_y) * F
     * where F = 1 / sqrt(1 + (theta_c * sigma_z / (2 * sigma_x))^2)
     * is the geometric reduction factor from the crossing angle.
     *
     * @param b1 beam 1
     * @param b2 beam 2
     * @param ip interaction point configuration
     * @return instantaneous luminosity [cm^-2 s^-1]
     */
    public static double instantaneous(Beam b1, Beam b2, InteractionPoint ip) {
        if (b1.getBunches().isEmpty() || b2.getBunches().isEmpty()) return 0;

        Bunch bunch1 = b1.getBunches().get(0);
        Bunch bunch2 = b2.getBunches().get(0);
        int numBunches = Math.min(b1.getFilledBuckets(), b2.getFilledBuckets());

        return instantaneousLuminosity(
                bunch1, bunch2, numBunches,
                ip.crossingAngle(), b1.getCircumference(),
                ip.betaStarX(), ip.betaStarY());
    }

    /**
     * Instantaneous luminosity at an IP with a finite crossing angle.
     *
     * @param bunch1        beam-1 bunch
     * @param bunch2        beam-2 bunch
     * @param numBunches    number of colliding bunch pairs
     * @param crossingAngle full crossing angle [rad]
     * @param circumference ring circumference [m]
     * @param betaStarX     horizontal β* at the IP [m]
     * @param betaStarY     vertical β* at the IP [m]
     * @return instantaneous luminosity [cm⁻²s⁻¹]
     */
    public static double instantaneousLuminosity(Bunch bunch1, Bunch bunch2,
                                                  int numBunches,
                                                  double crossingAngle,
                                                  double circumference,
                                                  double betaStarX,
                                                  double betaStarY) {
        double beta = bunch1.lorentzBeta();
        double fRev = C_LIGHT * beta / circumference;

        double geomEmitX1 = bunch1.geometricEmittanceX();
        double geomEmitX2 = bunch2.geometricEmittanceX();
        double geomEmitY1 = bunch1.geometricEmittanceY();
        double geomEmitY2 = bunch2.geometricEmittanceY();

        double sigmaX = Math.sqrt((geomEmitX1 * betaStarX + geomEmitX2 * betaStarX) / 2.0);
        double sigmaY = Math.sqrt((geomEmitY1 * betaStarY + geomEmitY2 * betaStarY) / 2.0);

        double sigmaZ = (bunch1.getSigmaZ() + bunch2.getSigmaZ()) / 2.0;
        double halfAngle = crossingAngle / 2.0;

        double phi = halfAngle * sigmaZ / sigmaX;
        double reductionFactor = 1.0 / Math.sqrt(1.0 + phi * phi);

        double n1 = bunch1.getNumParticles();
        double n2 = bunch2.getNumParticles();

        // L in m⁻²s⁻¹, multiply by 1e-4 to convert to cm⁻²s⁻¹
        double lumiM2 = n1 * n2 * fRev * numBunches
                / (4.0 * Math.PI * sigmaX * sigmaY) * reductionFactor;
        return lumiM2 * 1e-4;
    }

    /**
     * Mean number of pileup interactions per bunch crossing.
     *
     * @param luminosity instantaneous luminosity [cm⁻²s⁻¹]
     * @param sigmaInel  inelastic pp cross-section [cm²]
     * @param fRev       revolution frequency [Hz]
     * @param numBunches number of colliding bunch pairs
     * @return mean pileup ⟨μ⟩
     */
    public static double meanPileup(double luminosity, double sigmaInel,
                                     double fRev, int numBunches) {
        return luminosity * sigmaInel / (fRev * numBunches);
    }

    /**
     * Converts an integrated luminosity from cm⁻² to inverse femtobarns.
     * 1 fb⁻¹ = 10³⁹ cm⁻².
     */
    public static double toInverseFemtobarns(double cm2) {
        return cm2 * 1e-39;
    }

    /**
     * Converts an integrated luminosity from inverse femtobarns to cm⁻².
     */
    public static double fromInverseFemtobarns(double fb) {
        return fb / 1e-39;
    }
}
