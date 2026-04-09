package com.lhcsim.physics;

import com.lhcsim.physics.accelerator.Lattice;
import com.lhcsim.physics.accelerator.LatticeLoader;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.Twiss;
import com.lhcsim.physics.beam.TwissPropagator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Validates the optics engine against a precomputed reference Twiss table.
 * <p>
 * The reference file ({@code madx-reference/ps-twiss.tfs}) is in the standard
 * TFS (Table File System) format. If MAD-X is unavailable, the file is
 * checked into the repository so that the test runs in CI without external tools.
 * <p>
 * Tolerance: 1% for beta functions, 0.001 for tunes.
 */
class MadxReferenceTest {

    private static List<RefRow> refRows;
    private static double refQ1;
    private static double refQ2;

    private static Lattice ps;
    private static final double PS_ENERGY = 26.0;
    private static final double GAMMA_PS = PS_ENERGY / Bunch.PROTON_MASS;

    record RefRow(String name, String keyword, double s, double betaX, double betaY,
                  double alphaX, double alphaY) {}

    @BeforeAll
    static void loadReference() throws IOException {
        refRows = new ArrayList<>();

        try (InputStream is = MadxReferenceTest.class.getResourceAsStream(
                "/madx-reference/ps-twiss.tfs")) {
            assertThat(is).as("Reference TFS file must be present").isNotNull();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("@ Q1")) {
                    refQ1 = parseHeaderDouble(line);
                } else if (line.startsWith("@ Q2")) {
                    refQ2 = parseHeaderDouble(line);
                } else if (line.startsWith("\"") || line.matches("^\\s+\".*")) {
                    RefRow row = parseDataRow(line.trim());
                    if (row != null) refRows.add(row);
                }
            }
        }

        ps = LatticeLoader.loadResource("/data/lattices/ps.json");
    }

    @Test
    void testReferenceLoaded() {
        assertThat(refRows).isNotEmpty();
        assertThat(refRows.size()).isGreaterThan(100);
    }

    @Test
    void testTunesMatchReference() {
        double tuneX = ps.computeTuneX(GAMMA_PS);
        double tuneY = ps.computeTuneY(GAMMA_PS);

        assertThat(tuneX).isCloseTo(refQ1, within(0.001));
        assertThat(tuneY).isCloseTo(refQ2, within(0.001));
    }

    @Test
    void testBetaFunctionMatchesReference() {
        List<TwissPropagator.SamplePoint> computed =
                TwissPropagator.betaFunction(ps, GAMMA_PS);

        // Match every reference row against the closest computed sample by s
        int matched = 0;
        for (RefRow ref : refRows) {
            TwissPropagator.SamplePoint closest = null;
            double minDist = Double.MAX_VALUE;
            for (TwissPropagator.SamplePoint sp : computed) {
                double dist = Math.abs(sp.s() - ref.s);
                if (dist < minDist) {
                    minDist = dist;
                    closest = sp;
                }
            }
            if (closest != null && minDist < 0.2) {
                // 1% tolerance on beta
                double tolX = Math.max(0.01 * ref.betaX, 1e-6);
                double tolY = Math.max(0.01 * ref.betaY, 1e-6);

                assertThat(closest.betaX())
                        .as("betaX at s=%.2f (%s)", ref.s, ref.name)
                        .isCloseTo(ref.betaX, within(tolX));
                assertThat(closest.betaY())
                        .as("betaY at s=%.2f (%s)", ref.s, ref.name)
                        .isCloseTo(ref.betaY, within(tolY));
                matched++;
            }
        }

        // Ensure we matched at least 90% of reference points
        assertThat(matched).isGreaterThan((int) (refRows.size() * 0.9));
    }

    @Test
    void testPeriodicTwissClosureMatchesReference() {
        Twiss periodic = TwissPropagator.computePeriodic(ps, GAMMA_PS);

        // First reference row should match the periodic Twiss at s=0
        RefRow start = refRows.get(0);
        assertThat(periodic.betaX())
                .isCloseTo(start.betaX, within(0.01 * start.betaX));
        assertThat(periodic.betaY())
                .isCloseTo(start.betaY, within(0.01 * start.betaY));
    }

    // ── TFS parsing helpers ─────────────────────────────────────────

    private static double parseHeaderDouble(String line) {
        String[] parts = line.trim().split("\\s+");
        return Double.parseDouble(parts[parts.length - 1]);
    }

    private static RefRow parseDataRow(String line) {
        // Format: "NAME" "KEYWORD" S BETX BETY ALFX ALFY
        String[] parts = line.split("\\s+");
        if (parts.length < 7) return null;
        try {
            String name = parts[0].replace("\"", "");
            String keyword = parts[1].replace("\"", "");
            double s = Double.parseDouble(parts[2]);
            double betaX = Double.parseDouble(parts[3]);
            double betaY = Double.parseDouble(parts[4]);
            double alphaX = Double.parseDouble(parts[5]);
            double alphaY = Double.parseDouble(parts[6]);
            return new RefRow(name, keyword, s, betaX, betaY, alphaX, alphaY);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
