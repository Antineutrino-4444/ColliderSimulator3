package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatrixElementTest {

    @Test
    void drellYanNonZeroAtZPole() {
        DrellYan dy = new DrellYan();
        // Z-pole kinematics: √ŝ = mZ, back-to-back in CM frame
        double mZ = 91.1876;
        double halfE = mZ / 2;
        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = new FourMomentum(halfE, halfE * 0.6, 0, halfE * 0.8);
        FourMomentum p4 = new FourMomentum(halfE, -halfE * 0.6, 0, -halfE * 0.8);

        double m2 = dy.squared(p1, p2, p3, p4);
        assertThat(m2).isGreaterThan(0);
    }

    @Test
    void drellYanIncreasesNearZPole() {
        DrellYan dy = new DrellYan();
        // Off-pole: √ŝ = 50 GeV
        double offPole = computeDY(dy, 50);
        // On-pole: √ŝ = mZ
        double onPole = computeDY(dy, 91.1876);
        // The Z resonance should enhance the cross-section
        assertThat(onPole).isGreaterThan(offPole);
    }

    @Test
    void ggHiggsNonZeroNearHiggsMass() {
        GgHiggs gg = new GgHiggs();
        double mH = 125.25;
        double halfE = mH / 2;
        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = new FourMomentum(halfE, halfE * 0.5, 0, halfE * 0.866);
        FourMomentum p4 = new FourMomentum(halfE, -halfE * 0.5, 0, -halfE * 0.866);

        double m2 = gg.squared(p1, p2, p3, p4);
        assertThat(m2).isGreaterThan(0);
    }

    @Test
    void ttbarZeroBelowThreshold() {
        TTbar tt = new TTbar();
        // √ŝ < 2*mt = 345 GeV
        double halfE = 100; // √ŝ = 200 GeV
        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = new FourMomentum(halfE, halfE * 0.5, 0, halfE * 0.866);
        FourMomentum p4 = new FourMomentum(halfE, -halfE * 0.5, 0, -halfE * 0.866);

        double m2 = tt.squaredGG(p1, p2, p3, p4);
        assertThat(m2).isEqualTo(0.0);
    }

    @Test
    void ttbarNonZeroAboveThreshold() {
        TTbar tt = new TTbar();
        double mt = 172.69;
        double halfE = 1.2 * mt; // √ŝ = 2.4*mt, above threshold
        double pmag = Math.sqrt(halfE * halfE - mt * mt);
        double cosTheta = 0.5;
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);

        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = FourMomentum.fromMassAndMomentum(mt,
                pmag * sinTheta, 0, pmag * cosTheta);
        FourMomentum p4 = FourMomentum.fromMassAndMomentum(mt,
                -pmag * sinTheta, 0, -pmag * cosTheta);

        double m2gg = tt.squaredGG(p1, p2, p3, p4);
        double m2qq = tt.squaredQQ(p1, p2, p3, p4);

        assertThat(m2gg).isGreaterThan(0);
        assertThat(m2qq).isGreaterThan(0);
    }

    @Test
    void diPhotonNonZero() {
        DiPhoton dp = new DiPhoton();
        double halfE = 100; // √ŝ = 200 GeV
        double cosTheta = 0.5;
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);

        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = new FourMomentum(halfE, halfE * sinTheta, 0, halfE * cosTheta);
        FourMomentum p4 = new FourMomentum(halfE, -halfE * sinTheta, 0, -halfE * cosTheta);

        double m2 = dp.squared(p1, p2, p3, p4);
        assertThat(m2).isGreaterThan(0);
    }

    @Test
    void runningAlphaSDecreases() {
        // α_s should decrease with increasing Q²
        double as_low = TTbar.runningAlphaS(100);   // Q ~ 10 GeV
        double as_high = TTbar.runningAlphaS(10000); // Q ~ 100 GeV
        assertThat(as_low).isGreaterThan(as_high);
    }

    private double computeDY(DrellYan dy, double sqrtS) {
        double halfE = sqrtS / 2;
        FourMomentum p1 = new FourMomentum(halfE, 0, 0, halfE);
        FourMomentum p2 = new FourMomentum(halfE, 0, 0, -halfE);
        FourMomentum p3 = new FourMomentum(halfE, halfE * 0.6, 0, halfE * 0.8);
        FourMomentum p4 = new FourMomentum(halfE, -halfE * 0.6, 0, -halfE * 0.8);
        return dy.squared(p1, p2, p3, p4);
    }
}
