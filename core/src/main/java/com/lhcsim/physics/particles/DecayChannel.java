package com.lhcsim.physics.particles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * A single decay channel for a particle, specifying branching ratio and product PDG IDs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DecayChannel {

    private double branchingRatio;
    private List<Integer> products;
    private String label;

    /** Default constructor for Jackson deserialization. */
    public DecayChannel() {
    }

    @JsonCreator
    public DecayChannel(
            @JsonProperty("branchingRatio") double branchingRatio,
            @JsonProperty("products") List<Integer> products,
            @JsonProperty("label") String label) {
        this.branchingRatio = branchingRatio;
        this.products = products == null ? Collections.emptyList() : List.copyOf(products);
        this.label = label;
    }

    public double getBranchingRatio() {
        return branchingRatio;
    }

    public List<Integer> getProducts() {
        return products;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label + " (BR=" + branchingRatio + ", products=" + products + ")";
    }
}
