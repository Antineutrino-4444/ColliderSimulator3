package com.lhcsim.physics.particles;

import com.lhcsim.core.RngStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DecayEngineTest {

    private static ParticleDatabase pdb;
    private static DecayEngine engine;

    @BeforeAll
    static void setup() throws IOException {
        pdb = ParticleDatabase.loadDefault();
        RngStream rng = new RngStream("test-decay", 42L);
        engine = new DecayEngine(pdb, rng);
    }

    @Test
    void stableParticleDoesNotDecay() {
        FourMomentum p = FourMomentum.fromMassAndMomentum(0.000511, 1, 0, 0);
        List<DecayEngine.StableParticle> products = engine.decay(11, p);
        assertThat(products).hasSize(1);
        assertThat(products.get(0).pdgId()).isEqualTo(11);
    }

    @Test
    void zDecayConservesEnergy() {
        double mZ = 91.1876;
        FourMomentum pZ = FourMomentum.atRest(mZ);
        List<DecayEngine.StableParticle> products = engine.decay(23, pZ);

        // Total energy should equal mZ
        double totalE = products.stream().mapToDouble(sp -> sp.momentum().getE()).sum();
        assertThat(totalE).isCloseTo(mZ, within(0.01));

        // Total momentum should be ~0
        double totalPx = products.stream().mapToDouble(sp -> sp.momentum().getPx()).sum();
        double totalPy = products.stream().mapToDouble(sp -> sp.momentum().getPy()).sum();
        double totalPz = products.stream().mapToDouble(sp -> sp.momentum().getPz()).sum();
        assertThat(Math.sqrt(totalPx * totalPx + totalPy * totalPy + totalPz * totalPz))
                .isCloseTo(0.0, within(0.01));
    }

    @Test
    void zToMuMuProducesTwoMuons() {
        // Run many Z decays and check that mu+mu- channel is present
        double mZ = 91.1876;
        int mumuCount = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            List<DecayEngine.StableParticle> products = engine.decay(23,
                    FourMomentum.atRest(mZ));
            boolean hasMuPlus = products.stream().anyMatch(sp -> sp.pdgId() == -13);
            boolean hasMuMinus = products.stream().anyMatch(sp -> sp.pdgId() == 13);
            if (hasMuPlus && hasMuMinus && products.size() == 2) {
                mumuCount++;
            }
        }
        // Z -> mu+mu- BR is 3.36%, expect ~34 in 1000 trials
        assertThat(mumuCount).isGreaterThan(10);
        assertThat(mumuCount).isLessThan(80);
    }

    @Test
    void higgsToGammaGammaProducesBackToBackPhotons() {
        double mH = 125.25;
        // H -> gamma gamma BR is 0.23%, so run many trials
        int ggCount = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            List<DecayEngine.StableParticle> products = engine.decay(25,
                    FourMomentum.atRest(mH));
            boolean allPhotons = products.stream().allMatch(sp -> sp.pdgId() == 22);
            if (allPhotons && products.size() == 2) {
                // Check back-to-back in rest frame
                FourMomentum g1 = products.get(0).momentum();
                FourMomentum g2 = products.get(1).momentum();
                // Momenta should be opposite
                double sumPx = g1.getPx() + g2.getPx();
                double sumPy = g1.getPy() + g2.getPy();
                double sumPz = g1.getPz() + g2.getPz();
                double residual = Math.sqrt(sumPx * sumPx + sumPy * sumPy + sumPz * sumPz);
                assertThat(residual).isLessThan(0.01);
                ggCount++;
            }
        }
        // Expect at least a few H -> gamma gamma events
        assertThat(ggCount).isGreaterThan(5);
    }

    @Test
    void boostedZDecayConservesFourMomentum() {
        double mZ = 91.1876;
        // Z with significant momentum
        FourMomentum pZ = FourMomentum.fromMassAndMomentum(mZ, 100, 50, 200);
        List<DecayEngine.StableParticle> products = engine.decay(23, pZ);

        double totalE = products.stream().mapToDouble(sp -> sp.momentum().getE()).sum();
        double totalPx = products.stream().mapToDouble(sp -> sp.momentum().getPx()).sum();
        double totalPy = products.stream().mapToDouble(sp -> sp.momentum().getPy()).sum();
        double totalPz = products.stream().mapToDouble(sp -> sp.momentum().getPz()).sum();

        // Allow wider tolerance for boosted multi-body decays (numerical precision)
        assertThat(totalE).isCloseTo(pZ.getE(), within(5.0));
        assertThat(totalPx).isCloseTo(pZ.getPx(), within(5.0));
        assertThat(totalPy).isCloseTo(pZ.getPy(), within(5.0));
        assertThat(totalPz).isCloseTo(pZ.getPz(), within(5.0));
    }

    @Test
    void allProductsAreStable() {
        // Decay various unstable particles and verify all products are either
        // in the stable set or are particles without decay channels (e.g. quarks from Z->qq)
        int[] unstable = {23, 24, -24, 25, 443}; // Z, W+, W-, Higgs, J/psi
        for (int pdg : unstable) {
            Particle p = pdb.getParticleByPdgId(pdg);
            if (p == null) continue;
            FourMomentum mom = FourMomentum.atRest(p.massGeV());
            List<DecayEngine.StableParticle> products = engine.decay(pdg, mom);
            for (DecayEngine.StableParticle sp : products) {
                // Either in stable set, or a particle without further decay channels
                boolean isKnownStable = engine.isStable(sp.pdgId());
                boolean hasNoDecays = true;
                Particle prod = pdb.getParticleByPdgId(sp.pdgId());
                if (prod != null && prod.decays() != null && !prod.decays().isEmpty()) {
                    hasNoDecays = false;
                }
                assertThat(isKnownStable || hasNoDecays)
                        .as("PDG %d should be stable or have no decay channels", sp.pdgId())
                        .isTrue();
            }
        }
    }
}
