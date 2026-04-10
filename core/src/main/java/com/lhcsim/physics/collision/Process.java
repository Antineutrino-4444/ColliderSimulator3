package com.lhcsim.physics.collision;

/**
 * Enumeration of physics processes with their cross-section table keys
 * and human-readable names.
 * <p>
 * Sources: LHC Higgs Working Group YR4 (CERN-2017-002-M),
 * PDG 2024, TOTEM/ATLAS inelastic measurements.
 */
public enum Process {

    INELASTIC_PP("total_inelastic", "Inelastic pp",
            "Total inelastic pp cross-section. PDG 2024."),

    W_PROD("w_production", "W production",
            "Inclusive W boson production. NNLO QCD. FEWZ 3.1."),

    Z_PROD("z_production", "Z production",
            "Inclusive Z/gamma* production. NNLO QCD."),

    TTBAR("ttbar", "Top pair",
            "Top quark pair production. NNLO+NNLL. Top++ 2.0."),

    HIGGS_GGF("higgs_ggf", "Higgs (ggF)",
            "Higgs via gluon-gluon fusion. N3LO QCD. YR4."),

    HIGGS_VBF("higgs_vbf", "Higgs (VBF)",
            "Higgs via vector-boson fusion. NNLO QCD. YR4."),

    HIGGS_VH("higgs_vh", "Higgs (VH)",
            "Associated VH production. NNLO QCD. YR4."),

    HIGGS_TTH("higgs_tth", "Higgs (ttH)",
            "Associated ttH production. NLO QCD. YR4."),

    HH("di_higgs", "Di-Higgs",
            "Higgs pair production via ggF. NLO FTapprox. YR4."),

    DIBOSON_WW("diboson_ww", "WW",
            "Diboson WW production. NLO QCD."),

    DIBOSON_WZ("diboson_wz", "WZ",
            "Diboson WZ production. NLO QCD."),

    DIBOSON_ZZ("diboson_zz", "ZZ",
            "Diboson ZZ production. NLO QCD."),

    JPSI_PROD("jpsi_prod", "J/psi production",
            "Prompt J/psi production. Color-Evaporation Model."),

    BSM_PLACEHOLDER("bsm_placeholder", "BSM placeholder",
            "Placeholder for beyond-Standard-Model searches.");

    private final String key;
    private final String displayName;
    private final String citation;

    Process(String key, String displayName, String citation) {
        this.key = key;
        this.displayName = displayName;
        this.citation = citation;
    }

    /** Key used in the cross-section JSON table. */
    public String key() {
        return key;
    }

    /** Human-readable display name. */
    public String displayName() {
        return displayName;
    }

    /** Citation / source note. */
    public String citation() {
        return citation;
    }

    /**
     * Looks up a Process by its JSON key.
     *
     * @param key the cross-section table key
     * @return matching Process, or null if not found
     */
    public static Process fromKey(String key) {
        for (Process p : values()) {
            if (p.key.equals(key)) return p;
        }
        return null;
    }
}
