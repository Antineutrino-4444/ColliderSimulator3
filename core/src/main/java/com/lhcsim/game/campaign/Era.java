package com.lhcsim.game.campaign;

import java.util.List;

/**
 * Represents a campaign era — a distinct period in the history of particle
 * physics that the player progresses through.
 *
 * @param number         ordinal era number (1-based)
 * @param name           short display name
 * @param description    flavour text
 * @param beamTimePerYear available beam-time hours per in-game year
 * @param sqrtS          centre-of-mass energy [TeV]
 * @param activeMachine  accelerator name
 * @param missions       ordered list of mission identifiers
 * @param unlocks        component identifiers unlocked at era start
 */
public record Era(
        int number,
        String name,
        String description,
        double beamTimePerYear,
        double sqrtS,
        String activeMachine,
        List<String> missions,
        List<String> unlocks
) {
    /**
     * Creates the complete list of campaign eras in progression order.
     */
    public static List<Era> createAllEras() {
        return List.of(
                new Era(1, "Fixed Target",
                        "Pioneer era: fixed-target experiments at the SPS push the energy frontier.",
                        800, 0.546, "SPS",
                        List.of("discover_w_boson", "discover_z_boson"),
                        List.of("calorimeter_basic", "tracking_chamber")),
                new Era(2, "LHC Run 1",
                        "First collisions at the LHC open a new window on TeV-scale physics.",
                        1600, 8.0, "LHC",
                        List.of("discover_higgs", "measure_top_mass"),
                        List.of("silicon_tracker", "superconducting_magnets")),
                new Era(3, "LHC Run 2/3",
                        "Higher energy and luminosity enable precision Higgs measurements.",
                        2400, 13.6, "LHC",
                        List.of("higgs_coupling_ww", "higgs_coupling_bb", "search_bsm"),
                        List.of("pixel_detector", "upgraded_trigger")),
                new Era(4, "HL-LHC",
                        "The High-Luminosity upgrade delivers unprecedented datasets.",
                        3000, 14.0, "HL-LHC",
                        List.of("higgs_self_coupling", "rare_decays", "susy_search"),
                        List.of("high_granularity_cal", "timing_detector")),
                new Era(5, "FCC-hh",
                        "The Future Circular Collider reaches 100 TeV centre-of-mass energy.",
                        3600, 100.0, "FCC",
                        List.of("discover_heavy_higgs", "probe_dark_matter", "precision_ewk"),
                        List.of("fcc_magnets", "fcc_calorimeter"))
        );
    }
}
