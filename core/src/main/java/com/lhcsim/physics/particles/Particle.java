package com.lhcsim.physics.particles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Immutable particle data record following the PDG convention.
 * <p>
 * Masses and widths are in GeV/c^2 and GeV respectively.
 * Charge is in units of e/3 (so a proton has charge = +3).
 * <p>
 * Reference: PDG 2024 — <a href="https://pdg.lbl.gov/">pdg.lbl.gov</a>.
 *
 * @param pdgId         PDG Monte-Carlo ID
 * @param name          human-readable name
 * @param latex         LaTeX representation (or symbol)
 * @param massGeV       rest mass [GeV/c^2]
 * @param widthGeV      total decay width [GeV]
 * @param charge        electric charge in units of e/3
 * @param spin          spin quantum number
 * @param colorCharge   colour charge (0 for leptons/bosons, 3 for quarks)
 * @param decays        list of decay channels with branching ratios
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Particle(
        int pdgId,
        String name,
        String latex,
        double massGeV,
        double widthGeV,
        int charge,
        double spin,
        int colorCharge,
        List<DecayChannel> decays
) {

    /**
     * Jackson-compatible constructor that accepts both old and new JSON field names.
     */
    @JsonCreator
    public Particle(
            @JsonProperty("pdgId") int pdgId,
            @JsonProperty("name") String name,
            @JsonProperty("symbol") String latex,
            @JsonProperty("mass") double massGeV,
            @JsonProperty("width") double widthGeV,
            @JsonProperty("charge") int charge,
            @JsonProperty("spin") double spin,
            @JsonProperty("colorCharge") int colorCharge,
            @JsonProperty("decayChannels") List<DecayChannel> decays) {
        this.pdgId = pdgId;
        this.name = name;
        this.latex = latex != null ? latex : name;
        this.massGeV = massGeV;
        this.widthGeV = widthGeV;
        this.charge = charge;
        this.spin = spin;
        this.colorCharge = colorCharge;
        this.decays = decays == null ? List.of() : List.copyOf(decays);
    }

    /** Returns the electric charge in units of the elementary charge e. */
    public double electricCharge() {
        return charge / 3.0;
    }

    /** A particle is considered stable if it has zero or negligible width. */
    public boolean isStable() {
        return widthGeV == 0 || widthGeV < 1e-30;
    }
}
