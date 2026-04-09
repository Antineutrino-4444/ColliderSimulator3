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
    void testAtLeast60Particles() {
        assertThat(db.size()).isGreaterThanOrEqualTo(60);
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
        // Chunk 1.2: Higgs mass = 125.20 +/- 0.11 GeV
        assertThat(higgs.getMass()).isCloseTo(125.20, within(0.11));
    }

    @Test
    void testHiggsDiphotonBR() {
        ParticleData higgs = db.getByPdgId(25);
        assertThat(higgs).isNotNull();
        // Chunk 1.2: H->gamma gamma BR ~ 0.00227
        double diphotonBR = higgs.getDecayChannels().stream()
                .filter(dc -> dc.getLabel() != null && dc.getLabel().contains("gamma gamma"))
                .mapToDouble(DecayChannel::getBranchingRatio)
                .sum();
        assertThat(diphotonBR).isCloseTo(0.00227, within(0.0005));
    }

    @Test
    void testHiggsBranchingRatiosSum() {
        ParticleData higgs = db.getByPdgId(25);
        assertThat(higgs).isNotNull();
        double brSum = higgs.getDecayChannels().stream()
                .mapToDouble(DecayChannel::getBranchingRatio)
                .sum();
        assertThat(brSum).isCloseTo(1.0, within(0.01));
    }

    @Test
    void testAllBranchingRatiosSumToOne() {
        for (ParticleData p : db.getAllParticles()) {
            if (p.getDecayChannels() != null && !p.getDecayChannels().isEmpty()) {
                double sum = p.getDecayChannels().stream()
                        .mapToDouble(DecayChannel::getBranchingRatio)
                        .sum();
                assertThat(sum)
                        .as("BRs for %s (PDG %d)", p.getName(), p.getPdgId())
                        .isCloseTo(1.0, within(1e-3));
            }
        }
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

    @Test
    void testSingleton() {
        ParticleDatabase singleton = ParticleDatabase.getInstance();
        assertThat(singleton).isNotNull();
        assertThat(singleton.size()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void testParticleRecord() {
        Particle higgs = db.getParticleByPdgId(25);
        assertThat(higgs).isNotNull();
        assertThat(higgs.massGeV()).isCloseTo(125.20, within(0.11));
        assertThat(higgs.name()).isEqualTo("Higgs");
    }

    @Test
    void testJPsiPresent() {
        ParticleData jpsi = db.getByPdgId(443);
        assertThat(jpsi).isNotNull();
        assertThat(jpsi.getMass()).isCloseTo(3.097, within(0.01));
    }

    @Test
    void testUpsilonPresent() {
        ParticleData upsilon = db.getByPdgId(553);
        assertThat(upsilon).isNotNull();
        assertThat(upsilon.getMass()).isCloseTo(9.460, within(0.01));
    }

    @Test
    void testLambdaPresent() {
        ParticleData lambda = db.getByPdgId(3122);
        assertThat(lambda).isNotNull();
        assertThat(lambda.getMass()).isCloseTo(1.116, within(0.01));
    }

    @Test
    void testXiPresent() {
        ParticleData xi = db.getByPdgId(3322);
        assertThat(xi).isNotNull();
    }

    @Test
    void testBMesonPresent() {
        ParticleData bPlus = db.getByPdgId(521);
        assertThat(bPlus).isNotNull();
        assertThat(bPlus.getMass()).isCloseTo(5.279, within(0.01));
    }

    @Test
    void testDMesonPresent() {
        ParticleData dPlus = db.getByPdgId(411);
        assertThat(dPlus).isNotNull();
    }
}
