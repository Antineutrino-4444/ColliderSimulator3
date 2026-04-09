package com.lhcsim.physics.particles;

/**
 * Standard PDG Monte-Carlo particle ID codes.
 * <p>
 * Reference: PDG 2024 Monte-Carlo numbering scheme,
 * <a href="https://pdg.lbl.gov/2024/mcdata/mc_particle_id_contents.html">PDG MC IDs</a>.
 */
public final class PdgId {

    private PdgId() {}

    // ── Leptons ─────────────────────────────────────────────────────

    public static final int ELECTRON = 11;
    public static final int POSITRON = -11;
    public static final int ELECTRON_NEUTRINO = 12;
    public static final int MUON = 13;
    public static final int ANTIMUON = -13;
    public static final int MUON_NEUTRINO = 14;
    public static final int TAU = 15;
    public static final int ANTITAU = -15;
    public static final int TAU_NEUTRINO = 16;

    // ── Quarks ──────────────────────────────────────────────────────

    public static final int DOWN = 1;
    public static final int UP = 2;
    public static final int STRANGE = 3;
    public static final int CHARM = 4;
    public static final int BOTTOM = 5;
    public static final int TOP = 6;

    // ── Gauge bosons ────────────────────────────────────────────────

    public static final int GLUON = 21;
    public static final int PHOTON = 22;
    public static final int Z = 23;
    public static final int W_PLUS = 24;
    public static final int W_MINUS = -24;

    // ── Higgs ───────────────────────────────────────────────────────

    public static final int HIGGS = 25;

    // ── Light mesons ────────────────────────────────────────────────

    public static final int PI_PLUS = 211;
    public static final int PI_MINUS = -211;
    public static final int PI_ZERO = 111;
    public static final int K_PLUS = 321;
    public static final int K_MINUS = -321;
    public static final int K_ZERO = 311;
    public static final int K_ZERO_SHORT = 310;
    public static final int K_ZERO_LONG = 130;

    // ── Heavy mesons ────────────────────────────────────────────────

    public static final int D_PLUS = 411;
    public static final int D_MINUS = -411;
    public static final int D_ZERO = 421;
    public static final int D_S_PLUS = 431;
    public static final int B_PLUS = 521;
    public static final int B_MINUS = -521;
    public static final int B_ZERO = 511;
    public static final int B_S = 531;

    // ── Quarkonia ───────────────────────────────────────────────────

    public static final int J_PSI = 443;
    public static final int UPSILON_1S = 553;
    public static final int ETA_C = 441;

    // ── Baryons ─────────────────────────────────────────────────────

    public static final int PROTON = 2212;
    public static final int ANTIPROTON = -2212;
    public static final int NEUTRON = 2112;
    public static final int LAMBDA = 3122;
    public static final int SIGMA_PLUS = 3222;
    public static final int SIGMA_ZERO = 3212;
    public static final int SIGMA_MINUS = 3112;
    public static final int XI_ZERO = 3322;
    public static final int XI_MINUS = 3312;
    public static final int OMEGA_MINUS = 3334;
    public static final int LAMBDA_C = 4122;
    public static final int LAMBDA_B = 5122;
}
