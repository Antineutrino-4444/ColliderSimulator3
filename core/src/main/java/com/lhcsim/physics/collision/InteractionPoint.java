package com.lhcsim.physics.collision;

/**
 * Configuration for an interaction point (IP) where two beams cross.
 *
 * @param name          IP label, e.g. "IP1" (ATLAS), "IP5" (CMS)
 * @param crossingAngle full crossing angle [rad] (LHC: ~285 µrad)
 * @param betaStarX     horizontal β* at the IP [m] (LHC design: 0.55 m)
 * @param betaStarY     vertical β* at the IP [m] (LHC design: 0.55 m)
 */
public record InteractionPoint(
        String name,
        double crossingAngle,
        double betaStarX,
        double betaStarY
) {

    /**
     * LHC nominal IP1/IP5 configuration.
     * <p>
     * Reference: LHC Design Report Vol. 1, CERN-2004-003.
     */
    public static InteractionPoint lhcNominal(String name) {
        return new InteractionPoint(name, 285e-6, 0.55, 0.55);
    }

    /**
     * HL-LHC IP configuration with β* = 0.15 m.
     */
    public static InteractionPoint hlLhc(String name) {
        return new InteractionPoint(name, 510e-6, 0.15, 0.15);
    }
}
