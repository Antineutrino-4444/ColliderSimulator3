package com.lhcsim.physics.collision;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CrossSectionTableTest {

    private static CrossSectionTable table;

    @BeforeAll
    static void load() throws IOException {
        table = CrossSectionTable.loadDefault();
    }

    @Test
    void testHasProcesses() {
        assertThat(table.getAllProcesses().size()).isGreaterThan(5);
    }

    @Test
    void testHiggsGGF() {
        double xs = table.getCrossSectionAt("higgs_ggf", 13.0);
        assertThat(xs).isCloseTo(44.1, within(5.0));
    }

    @Test
    void testTtbar() {
        double xs = table.getCrossSectionAt("ttbar", 13.6);
        assertThat(xs).isCloseTo(923.0, within(50.0));
    }

    @Test
    void testTotalInelastic() {
        double xs = table.getCrossSectionAt("total_inelastic", 13.6);
        assertThat(xs).isCloseTo(80e9, within(5e9));
    }

    @Test
    void testExpectedEvents() {
        double events = table.expectedEvents("higgs_ggf", 13.0, 1000.0);
        // 44.1 pb * 1000 pb^-1 = ~44100
        assertThat(events).isCloseTo(44100.0, within(5000.0));
    }
}
