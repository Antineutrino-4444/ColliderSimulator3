package com.lhcsim.physics.collision.pdf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PdfGridTest {

    private static PdfGrid pdf;

    @BeforeAll
    static void loadGrid() throws IOException {
        pdf = PdfGrid.loadDefault();
    }

    @Test
    void gluonDominatesAtLowX() {
        // At x = 1e-3 and Q^2 = 10000 GeV^2, gluon should dominate over any single quark
        double xfGluon = pdf.xfx(21, 1e-3, 10000);
        double xfU = pdf.xfx(2, 1e-3, 10000);
        double xfD = pdf.xfx(1, 1e-3, 10000);
        assertThat(xfGluon).isGreaterThan(xfU);
        assertThat(xfGluon).isGreaterThan(xfD);
    }

    @Test
    void valenceUIntegratesToAboutTwo() {
        // Integrate (u - ubar) in ln(x) space at Q^2 ~ 10 GeV^2
        double q2 = 10.0;
        double[] xGrid = pdf.getXGrid();
        double integral = 0;
        for (int i = 0; i < xGrid.length - 1; i++) {
            double dlnx = Math.log(xGrid[i + 1]) - Math.log(xGrid[i]);
            double uv_i = pdf.xfx(2, xGrid[i], q2) - pdf.xfx(-2, xGrid[i], q2);
            double uv_ip = pdf.xfx(2, xGrid[i + 1], q2) - pdf.xfx(-2, xGrid[i + 1], q2);
            integral += 0.5 * (uv_i + uv_ip) * dlnx;
        }
        // Within 5% of 2.0
        assertThat(integral).isCloseTo(2.0, within(0.10));
    }

    @Test
    void valenceDIntegratesToAboutOne() {
        double q2 = 10.0;
        double[] xGrid = pdf.getXGrid();
        double integral = 0;
        for (int i = 0; i < xGrid.length - 1; i++) {
            double dlnx = Math.log(xGrid[i + 1]) - Math.log(xGrid[i]);
            double dv_i = pdf.xfx(1, xGrid[i], q2) - pdf.xfx(-1, xGrid[i], q2);
            double dv_ip = pdf.xfx(1, xGrid[i + 1], q2) - pdf.xfx(-1, xGrid[i + 1], q2);
            integral += 0.5 * (dv_i + dv_ip) * dlnx;
        }
        // Within 5% of 1.0
        assertThat(integral).isCloseTo(1.0, within(0.10));
    }

    @Test
    void charmVanishesBelowThreshold() {
        // Charm threshold is at Q^2 ~ m_c^2 ~ 1.69^2 ~ 2.85 GeV^2
        double xfCharm = pdf.xfx(4, 0.1, 1.0);
        assertThat(xfCharm).isEqualTo(0.0);
    }

    @Test
    void bottomVanishesBelowThreshold() {
        // Bottom threshold at Q^2 ~ m_b^2 ~ 4.18^2 ~ 17.5 GeV^2
        double xfBottom = pdf.xfx(5, 0.1, 1.0);
        assertThat(xfBottom).isEqualTo(0.0);
    }

    @Test
    void pdfValuesAreNonNegative() {
        int[] partons = {21, 2, -2, 1, -1, 3, -3, 4, 5};
        for (int pdg : partons) {
            for (double x : new double[]{1e-4, 1e-3, 0.01, 0.1, 0.5}) {
                for (double q2 : new double[]{10, 100, 10000}) {
                    assertThat(pdf.xfx(pdg, x, q2)).isGreaterThanOrEqualTo(0.0);
                }
            }
        }
    }

    @Test
    void unknownPartonReturnsZero() {
        assertThat(pdf.xfx(999, 0.1, 100)).isEqualTo(0.0);
    }

    @Test
    void interpolationIsContinuous() {
        // Check that nearby x values give similar results
        double q2 = 1000;
        double x1 = 0.05;
        double x2 = 0.0501;
        double f1 = pdf.xfx(21, x1, q2);
        double f2 = pdf.xfx(21, x2, q2);
        assertThat(Math.abs(f1 - f2) / f1).isLessThan(0.01);
    }
}
