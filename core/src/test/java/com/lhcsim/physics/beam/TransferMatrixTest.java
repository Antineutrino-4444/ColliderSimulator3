package com.lhcsim.physics.beam;

import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link TransferMatrix} and {@link PhaseSpace}.
 */
class TransferMatrixTest {

    private static final double GAMMA = 7461.0;

    @Test
    void testDriftAngle() {
        TransferMatrix d = TransferMatrix.drift(10.0, GAMMA);
        assertThat(d.get(0, 1)).isCloseTo(10.0, within(1e-12));
    }

    @Test
    void testDriftSymplectic() {
        TransferMatrix d = TransferMatrix.drift(5.0, GAMMA);
        // det of horizontal 2x2 block: M00*M11 - M01*M10 = 1
        double det = d.get(0, 0) * d.get(1, 1) - d.get(0, 1) * d.get(1, 0);
        assertThat(det).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void testThinQuadKicks() {
        double kl = 0.3;
        TransferMatrix q = TransferMatrix.thinQuadrupole(kl);
        assertThat(q.get(1, 0)).isCloseTo(-kl, within(1e-12));
        assertThat(q.get(3, 2)).isCloseTo(kl, within(1e-12));
    }

    @Test
    void testFodoCellStable() {
        double kl = 0.3;
        TransferMatrix qf = TransferMatrix.thinQuadrupole(kl);
        TransferMatrix qd = TransferMatrix.thinQuadrupole(-kl);
        TransferMatrix d = TransferMatrix.drift(5.0, GAMMA);

        // FODO: QF - drift - QD - drift
        TransferMatrix fodo = qf.multiply(d).multiply(qd).multiply(d);
        double traceX = fodo.get(0, 0) + fodo.get(1, 1);
        assertThat(Math.abs(traceX)).isLessThan(2.0);
    }

    @Test
    void testTwissTransportInvariant() {
        TransferMatrix d = TransferMatrix.drift(10.0, GAMMA);
        TwissParameters tw0 = new TwissParameters(10.0, -1.0, 10.0, -1.0, 0.0, 0.0);
        TwissParameters tw1 = d.transportTwiss(tw0);

        double invariant = tw1.getBetaX() * tw1.gammaX() - tw1.getAlphaX() * tw1.getAlphaX();
        assertThat(invariant).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void testMultiplyAssociative() {
        TransferMatrix a = TransferMatrix.drift(3.0, GAMMA);
        TransferMatrix b = TransferMatrix.thinQuadrupole(0.2);
        TransferMatrix c = TransferMatrix.drift(4.0, GAMMA);

        TransferMatrix ab_c = a.multiply(b).multiply(c);
        TransferMatrix a_bc = a.multiply(b.multiply(c));

        for (int r = 0; r < 6; r++) {
            for (int col = 0; col < 6; col++) {
                assertThat(ab_c.get(r, col)).isCloseTo(a_bc.get(r, col), within(1e-10));
            }
        }
    }

    // ── Chunk 1.3 additional tests ──────────────────────────────────

    @Test
    void testDriftComposedTwiceEqualsDoubleLength() {
        TransferMatrix d5 = TransferMatrix.drift(5.0, GAMMA);
        TransferMatrix d10 = TransferMatrix.drift(10.0, GAMMA);
        TransferMatrix d5x2 = d5.compose(d5);

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                assertThat(d5x2.get(r, c)).isCloseTo(d10.get(r, c), within(1e-12));
            }
        }
    }

    @Test
    void testQuadFollowedByInverseIsIdentity() {
        TransferMatrix q = TransferMatrix.thickQuadrupole(2.0, 0.5, GAMMA);
        TransferMatrix qInv = q.inverse();
        TransferMatrix product = q.compose(qInv);

        TransferMatrix id = TransferMatrix.identity();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                assertThat(product.get(r, c)).isCloseTo(id.get(r, c), within(1e-10));
            }
        }
    }

    @Test
    void testSymplecticity() {
        // M^T J M = J where J is the 6×6 symplectic form
        TransferMatrix m = TransferMatrix.thickQuadrupole(2.0, 0.5, GAMMA);
        SimpleMatrix mt = m.getMatrix();
        SimpleMatrix mtT = mt.transpose();

        // Build J: the symplectic form for 3 pairs (x,x'), (y,y'), (z,dp)
        SimpleMatrix j = new SimpleMatrix(6, 6);
        for (int i = 0; i < 3; i++) {
            j.set(2 * i, 2 * i + 1, 1.0);
            j.set(2 * i + 1, 2 * i, -1.0);
        }

        SimpleMatrix result = mtT.mult(j).mult(mt);

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                assertThat(result.get(r, c)).isCloseTo(j.get(r, c), within(1e-10));
            }
        }
    }

    @Test
    void testApplyPhaseSpace() {
        TransferMatrix d = TransferMatrix.drift(10.0, GAMMA);
        PhaseSpace ps = new PhaseSpace(0.001, 0.0001, 0.0, 0.0, 0.0, 0.0);
        PhaseSpace result = d.apply(ps);

        // x_out = x_in + L * x'_in = 0.001 + 10 * 0.0001 = 0.002
        assertThat(result.x()).isCloseTo(0.002, within(1e-12));
        assertThat(result.xp()).isCloseTo(0.0001, within(1e-12));
    }

    @Test
    void testIdentityApply() {
        PhaseSpace ps = new PhaseSpace(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        PhaseSpace result = TransferMatrix.identity().apply(ps);
        assertThat(result.x()).isCloseTo(1.0, within(1e-12));
        assertThat(result.yp()).isCloseTo(4.0, within(1e-12));
        assertThat(result.dp()).isCloseTo(6.0, within(1e-12));
    }

    @Test
    void testDeterminant() {
        TransferMatrix d = TransferMatrix.drift(5.0, GAMMA);
        assertThat(d.det()).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void testIdentityInverse() {
        TransferMatrix id = TransferMatrix.identity();
        TransferMatrix inv = id.inverse();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                assertThat(inv.get(r, c)).isCloseTo(id.get(r, c), within(1e-12));
            }
        }
    }
}
