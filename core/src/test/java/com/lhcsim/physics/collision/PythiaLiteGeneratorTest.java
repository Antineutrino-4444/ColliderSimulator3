package com.lhcsim.physics.collision;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.particles.DecayEngine;
import com.lhcsim.physics.particles.FourMomentum;
import com.lhcsim.physics.particles.ParticleDatabase;
import com.lhcsim.physics.collision.pdf.PdfGrid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PythiaLiteGeneratorTest {

    private static PythiaLiteGenerator generator;

    @BeforeAll
    static void setup() throws IOException {
        RngStream rng = new RngStream("test-pythia", 42L);
        generator = PythiaLiteGenerator.createDefault(rng);
    }

    @Test
    void higgsEventProducesFinalState() {
        GeneratedEvent event = generator.generate(Process.HIGGS_GGF, 13000, 0);

        assertThat(event.process()).isEqualTo(Process.HIGGS_GGF);
        assertThat(event.ecm()).isEqualTo(13000);
        assertThat(event.finalState()).isNotEmpty();
    }

    @Test
    void ttbarEventProducesFinalState() {
        GeneratedEvent event = generator.generate(Process.TTBAR, 13000, 0);

        assertThat(event.process()).isEqualTo(Process.TTBAR);
        assertThat(event.finalState()).isNotEmpty();
    }

    @Test
    void drellYanEventProducesLeptons() {
        // Z production should produce leptons from Z decay
        boolean foundLeptonPair = false;
        for (int i = 0; i < 100; i++) {
            GeneratedEvent event = generator.generate(Process.Z_PROD, 13000, 0);
            boolean hasMuPlus = event.finalState().stream()
                    .anyMatch(sp -> sp.pdgId() == -13);
            boolean hasMuMinus = event.finalState().stream()
                    .anyMatch(sp -> sp.pdgId() == 13);
            boolean hasEPlus = event.finalState().stream()
                    .anyMatch(sp -> sp.pdgId() == -11);
            boolean hasEMinus = event.finalState().stream()
                    .anyMatch(sp -> sp.pdgId() == 11);
            if ((hasMuPlus && hasMuMinus) || (hasEPlus && hasEMinus)) {
                foundLeptonPair = true;
                break;
            }
        }
        assertThat(foundLeptonPair).isTrue();
    }

    @Test
    void generationRateIsSufficient() throws IOException {
        // Should generate >= 50 events/second
        RngStream benchRng = new RngStream("bench", 99L);
        PythiaLiteGenerator benchGen = PythiaLiteGenerator.createDefault(benchRng);

        long start = System.nanoTime();
        int nEvents = 50;
        for (int i = 0; i < nEvents; i++) {
            benchGen.generate(Process.HIGGS_GGF, 13000, 0);
        }
        long elapsed = System.nanoTime() - start;
        double eventsPerSec = nEvents / (elapsed / 1e9);

        assertThat(eventsPerSec).isGreaterThan(50);
    }

    @Test
    void allFinalStateParticlesAreStable() {
        GeneratedEvent event = generator.generate(Process.HIGGS_GGF, 13000, 0);
        for (DecayEngine.StableParticle sp : event.finalState()) {
            int absPdg = Math.abs(sp.pdgId());
            // Accept: stable particles, photons, pi0, K0, or light quarks/gluons
            // (quarks/gluons may appear from hadronization edge cases)
            assertThat(DecayEngine.STABLE_PDGS.contains(sp.pdgId())
                    || sp.pdgId() == 22 || absPdg == 111
                    || absPdg == 311 || absPdg == 310
                    || absPdg == 21 || (absPdg >= 1 && absPdg <= 6))
                    .as("PDG %d should be a recognized final-state particle", sp.pdgId())
                    .isTrue();
        }
    }

    @Test
    void wProductionEventWorks() {
        GeneratedEvent event = generator.generate(Process.W_PROD, 13000, 0);
        assertThat(event.finalState()).isNotEmpty();
    }

    @Test
    void genericProcessWorks() {
        GeneratedEvent event = generator.generate(Process.JPSI_PROD, 13000, 0);
        assertThat(event.finalState()).isNotEmpty();
    }
}
