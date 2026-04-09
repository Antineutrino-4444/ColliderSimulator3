package com.lhcsim.physics;

/**
 * Fundamental physical constants used throughout the simulation.
 * <p>
 * All values are in SI units (metres, kilograms, seconds, Tesla, etc.)
 * unless noted otherwise in the individual constant's Javadoc.
 * <p>
 * Sources:
 * <ul>
 *   <li>CODATA 2018 — <a href="https://physics.nist.gov/cuu/Constants/">NIST</a></li>
 *   <li>PDG 2024 — <a href="https://pdg.lbl.gov/">Particle Data Group</a></li>
 * </ul>
 */
public final class Constants {

    private Constants() {}

    // ── Fundamental constants (CODATA 2018, exact or best-fit) ──────

    /** Speed of light in vacuum [m/s]. Exact. */
    public static final double c = 299_792_458.0;

    /** Elementary charge [C]. Exact since 2019 SI revision. */
    public static final double e = 1.602_176_634e-19;

    /** Reduced Planck constant h-bar [J·s]. */
    public static final double h_bar = 1.054_571_817e-34;

    /** Vacuum permittivity epsilon_0 [F/m]. */
    public static final double epsilon_0 = 8.854_187_8128e-12;

    /** Vacuum permeability mu_0 [N/A^2]. */
    public static final double mu_0 = 1.256_637_062_12e-6;

    // ── Particle masses (CODATA 2018 / PDG 2024) ───────────────────

    /** Electron mass [kg]. */
    public static final double m_e = 9.109_383_7015e-31;

    /** Proton mass [kg]. */
    public static final double m_p = 1.672_621_923_69e-27;

    /** Neutron mass [kg]. */
    public static final double m_n = 1.674_927_498_04e-27;

    // ── Unit conversions ────────────────────────────────────────────

    /** Conversion factor: 1 eV in joules [J/eV]. */
    public static final double eV_TO_J = e;

    /** Conversion factor: 1 joule in eV [eV/J]. */
    public static final double J_TO_eV = 1.0 / e;

    /** Conversion factor: 1 barn in m^2 [m^2/barn]. */
    public static final double BARN_TO_M2 = 1e-28;

    /** Conversion factor: 1 femtobarn in barns [barn/fb]. */
    public static final double FB_TO_BARN = 1e-15;

    // ── Derived helpers ─────────────────────────────────────────────

    /** Proton rest-mass energy [MeV]. */
    public static final double PROTON_MASS_MEV = m_p * c * c / (eV_TO_J * 1e6);

    /** Proton rest-mass energy [GeV]. */
    public static final double PROTON_MASS_GEV = PROTON_MASS_MEV / 1e3;

    /** Electron rest-mass energy [MeV]. */
    public static final double ELECTRON_MASS_MEV = m_e * c * c / (eV_TO_J * 1e6);
}
