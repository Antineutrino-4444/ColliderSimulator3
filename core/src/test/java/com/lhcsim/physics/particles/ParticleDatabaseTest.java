package com.lhcsim.physics.particles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ParticleDatabaseTest {

    private static ParticleDatabase db;

    @BeforeAll
    static void loadDatabase() throws IOException {
        db = ParticleDatabase.loadDefault();
    }

    @Test
    void testSize() {
        assertThat(db.size()).isGreaterThan(20);
    }

    @Test
    void testProtonMass() {
        ParticleData proton = db.getByPdgId(2212);
        assertThat(proton).isNotNull();
        assertThat(proton.getMass()).isCloseTo(0.938272, within(0.001));
    }

    @Test
    void testElectronMass() {
        ParticleData electron = db.getByPdgId(11);
        assertThat(electron).isNotNull();
        assertThat(electron.getMass()).isCloseTo(0.000511, within(0.00001));
    }

    @Test
    void testHiggsMass() {
        ParticleData higgs = db.getByPdgId(25);
        assertThat(higgs).isNotNull();
        assertThat(higgs.getMass()).isCloseTo(125.25, within(0.5));
    }

    @Test
    void testHiggsBranchingRatios() {
        ParticleData higgs = db.getByPdgId(25);
        assertThat(higgs).isNotNull();
        double brSum = higgs.getDecayChannels().stream()
                .mapToDouble(DecayChannel::getBranchingRatio)
                .sum();
        assertThat(brSum).isCloseTo(1.0, within(0.01));
    }

    @Test
    void testWMass() {
        ParticleData w = db.getByPdgId(24);
        assertThat(w).isNotNull();
        assertThat(w.getMass()).isCloseTo(80.377, within(0.05));
    }

    @Test
    void testZMass() {
        ParticleData z = db.getByPdgId(23);
        assertThat(z).isNotNull();
        assertThat(z.getMass()).isCloseTo(91.1876, within(0.01));
    }

    @Test
    void testTopMass() {
        ParticleData top = db.getByPdgId(6);
        assertThat(top).isNotNull();
        assertThat(top.getMass()).isCloseTo(172.76, within(1.0));
    }

    @Test
    void testLookupByName() {
        ParticleData higgs = db.getByName("Higgs");
        assertThat(higgs).isNotNull();
        assertThat(higgs.getPdgId()).isEqualTo(25);
    }
}
