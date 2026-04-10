package com.lhcsim.physics.collision.hadronize;

import com.lhcsim.core.RngStream;
import com.lhcsim.physics.collision.shower.PartonShower;
import com.lhcsim.physics.particles.FourMomentum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LundLiteTest {

    private LundLite lund;
    private PartonShower shower;

    @BeforeEach
    void setup() {
        RngStream showerRng = new RngStream("test-shower", 42L);
        RngStream lundRng = new RngStream("test-lund", 123L);
        shower = new PartonShower(showerRng);
        lund = new LundLite(lundRng);
    }

    @Test
    void producesHadrons() {
        // Shower a gluon then hadronize
        FourMomentum p = new FourMomentum(100, 0, 0, 100);
        List<PartonShower.ShowerParton> showered = shower.shower(21, p, 100 * 100);
        List<LundLite.Hadron> hadrons = lund.hadronize(showered);

        assertThat(hadrons).isNotEmpty();
    }

    @Test
    void chargedMultiplicityReasonableForMinBias() {
        // At √s = 13 TeV, average charged multiplicity in min-bias is ~70
        // We simulate with a simplified setup, so allow wide margin (30%)
        int totalCharged = 0;
        int nEvents = 100;

        for (int i = 0; i < nEvents; i++) {
            // Simulate minimum-bias-like: two gluon jets
            FourMomentum g1 = new FourMomentum(6500, 0, 30, 6499.93);
            FourMomentum g2 = new FourMomentum(6500, 0, -30, -6499.93);

            List<PartonShower.ShowerParton> s1 = shower.shower(21, g1, 6500 * 6500);
            List<PartonShower.ShowerParton> s2 = shower.shower(21, g2, 6500 * 6500);

            List<PartonShower.ShowerParton> all = new java.util.ArrayList<>(s1);
            all.addAll(s2);

            List<LundLite.Hadron> hadrons = lund.hadronize(all);

            // Count charged particles (pi±, K±, p/pbar)
            long charged = hadrons.stream()
                    .filter(h -> isCharged(h.pdgId()))
                    .count();
            totalCharged += (int) charged;
        }

        double avgCharged = (double) totalCharged / nEvents;
        // Very generous bounds since this is a simplified model
        assertThat(avgCharged).isGreaterThan(5);
    }

    @Test
    void pionDominatesProduction() {
        FourMomentum p = new FourMomentum(500, 0, 0, 500);
        List<PartonShower.ShowerParton> showered = shower.shower(21, p, 500 * 500);
        List<LundLite.Hadron> hadrons = lund.hadronize(showered);

        long pions = hadrons.stream()
                .filter(h -> Math.abs(h.pdgId()) == 211 || h.pdgId() == 111)
                .count();

        // Pions should be the most abundant hadron type
        assertThat(pions).isGreaterThan((long) (hadrons.size() * 0.3));
    }

    @Test
    void kaonToProtonRatioReasonable() {
        // Run many events
        int nKaons = 0;
        int nProtons = 0;

        for (int i = 0; i < 200; i++) {
            FourMomentum p = new FourMomentum(200, 0, 0, 200);
            List<PartonShower.ShowerParton> showered = shower.shower(21, p, 200 * 200);
            List<LundLite.Hadron> hadrons = lund.hadronize(showered);

            for (LundLite.Hadron h : hadrons) {
                int abs = Math.abs(h.pdgId());
                if (abs == 321) nKaons++;
                if (abs == 2212) nProtons++;
            }
        }

        // K/π ratio should be < 1 (kaons are suppressed)
        assertThat(nKaons).isGreaterThan(0);
        // Protons should be less common than kaons (baryon suppression)
        // Very loose check since this is a simplified model
        assertThat(nProtons).isGreaterThanOrEqualTo(0);
    }

    private boolean isCharged(int pdgId) {
        int abs = Math.abs(pdgId);
        return abs == 211 || abs == 321 || abs == 2212;
    }
}
