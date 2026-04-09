package com.lhcsim.physics.particles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Immutable PDG particle data record loaded from the particle database JSON.
 * <p>
 * Masses are in GeV/c², widths in GeV, lifetimes in seconds, and charge is
 * stored in units of e/3 (so a proton has charge = +3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticleData {

    private int pdgId;
    private String name;
    private String symbol;
    private double mass;
    private double width;
    private double lifetime;
    private int charge;
    private double spin;
    private String type;
    private List<DecayChannel> decayChannels;

    /** Default constructor for Jackson deserialization. */
    public ParticleData() {
    }

    @JsonCreator
    public ParticleData(
            @JsonProperty("pdgId") int pdgId,
            @JsonProperty("name") String name,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("mass") double mass,
            @JsonProperty("width") double width,
            @JsonProperty("lifetime") double lifetime,
            @JsonProperty("charge") int charge,
            @JsonProperty("spin") double spin,
            @JsonProperty("type") String type,
            @JsonProperty("decayChannels") List<DecayChannel> decayChannels) {
        this.pdgId = pdgId;
        this.name = name;
        this.symbol = symbol;
        this.mass = mass;
        this.width = width;
        this.lifetime = lifetime;
        this.charge = charge;
        this.spin = spin;
        this.type = type;
        this.decayChannels = decayChannels == null
                ? Collections.emptyList()
                : List.copyOf(decayChannels);
    }

    // ── Derived properties ──────────────────────────────────────────

    /** A particle is considered stable if it has zero width or an extremely long lifetime. */
    public boolean isStable() {
        return width == 0 || lifetime >= 1e30;
    }

    /** Returns the electric charge in units of the elementary charge e. */
    public double electricCharge() {
        return charge / 3.0;
    }

    // ── Getters ─────────────────────────────────────────────────────

    public int getPdgId() {
        return pdgId;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getMass() {
        return mass;
    }

    public double getWidth() {
        return width;
    }

    public double getLifetime() {
        return lifetime;
    }

    public int getCharge() {
        return charge;
    }

    public double getSpin() {
        return spin;
    }

    public String getType() {
        return type;
    }

    public List<DecayChannel> getDecayChannels() {
        return decayChannels;
    }

    @Override
    public String toString() {
        return symbol + " [" + pdgId + "] m=" + mass + " GeV";
    }
}
