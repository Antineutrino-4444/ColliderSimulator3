package com.lhcsim.physics.collision;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CrossSectionDatabaseTest {

    private static CrossSectionDatabase db;

    @BeforeAll
    static void load() throws IOException {
        db = CrossSectionDatabase.loadDefault();
    }

    @Test
    void testHiggsGgf13_6TeV() {
        double xs = db.get(Process.HIGGS_GGF, 13.6);
        assertThat(xs).isCloseTo(48.6, within(1.0));
    }

    @Test
    void testTtbar13TeV() {
        double xs = db.get(Process.TTBAR, 13.0);
        assertThat(xs).isCloseTo(832.0, within(20.0));
    }

    @Test
    void testInelastic13TeV() {
        double xs = db.get(Process.INELASTIC_PP, 13.0);
        // 80 mb = 80e9 pb
        assertThat(xs).isCloseTo(78.0e9, within(5.0e9));
    }

    @Test
    void testHiggsVh() {
        double xs = db.get(Process.HIGGS_VH, 13.0);
        assertThat(xs).isCloseTo(2.26, within(0.5));
    }

    @Test
    void testHiggsTth() {
        double xs = db.get(Process.HIGGS_TTH, 13.0);
        assertThat(xs).isCloseTo(0.507, within(0.1));
    }

    @Test
    void testInterpolation() {
        // 10 TeV is between 8 and 13 — should give a reasonable value
        double xs = db.get(Process.HIGGS_GGF, 10.0);
        assertThat(xs).isBetween(19.0, 44.0);
    }

    @Test
    void testExtrapolationGuard() {
        // 500 TeV is way outside the tabulated range (max 100)
        assertThatThrownBy(() -> db.get(Process.HIGGS_GGF, 500.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testExpectedEvents() {
        // 48.6 pb * 1000 pb^-1 = ~48600 events
        double events = db.expectedEvents(Process.HIGGS_GGF, 13.6, 1000.0);
        assertThat(events).isCloseTo(48600.0, within(5000.0));
    }
}
