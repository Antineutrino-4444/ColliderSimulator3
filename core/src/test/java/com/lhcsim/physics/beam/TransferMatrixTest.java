package com.lhcsim.physics.beam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
}
