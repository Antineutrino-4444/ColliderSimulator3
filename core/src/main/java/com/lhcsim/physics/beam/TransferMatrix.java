package com.lhcsim.physics.beam;

import org.ejml.simple.SimpleMatrix;

/**
 * 6×6 linear beam-transport (transfer) matrix for the phase-space vector
 * <b>(x, x', y, y', z, δ)</b>.
 * <p>
 * Each static factory method constructs the matrix for a single beam-line
 * element.  Matrices are composed with {@link #multiply(TransferMatrix)} to
 * build the one-turn map or a section transfer matrix.
 */
public class TransferMatrix {

    private final SimpleMatrix matrix;

    private TransferMatrix(SimpleMatrix matrix) {
        this.matrix = matrix;
    }

    // ── Static factories ────────────────────────────────────────────

    /** 6×6 identity matrix (no transformation). */
    public static TransferMatrix identity() {
        return new TransferMatrix(SimpleMatrix.identity(6));
    }

    /**
     * Field-free drift space of length <em>L</em>.
     * <pre>
     *   x  += L · x'
     *   y  += L · y'
     *   z  += L · δ / γ²
     * </pre>
     *
     * @param length drift length [m]
     * @param gamma  Lorentz gamma of the reference particle
     */
    public static TransferMatrix drift(double length, double gamma) {
        SimpleMatrix m = SimpleMatrix.identity(6);
        m.set(0, 1, length);
        m.set(2, 3, length);
        m.set(4, 5, length / (gamma * gamma));
        return new TransferMatrix(m);
    }

    /**
     * Thin (zero-length) quadrupole kick.
     * <p>
     * A positive <em>kl</em> focuses in the horizontal plane (defocuses
     * vertically) and vice-versa.
     *
     * @param kl integrated normalised gradient k·L [1/m]
     */
    public static TransferMatrix thinQuadrupole(double kl) {
        SimpleMatrix m = SimpleMatrix.identity(6);
        m.set(1, 0, -kl);
        m.set(3, 2, kl);
        return new TransferMatrix(m);
    }

    /**
     * Thick quadrupole with finite length and normalised gradient <em>k</em>.
     * <p>
     * Uses cos/sin transport for the focusing plane and cosh/sinh for the
     * defocusing plane. For k &gt; 0 the horizontal plane is focusing.
     *
     * @param length magnet length [m]
     * @param k      normalised gradient k = (1/Bρ)·(∂B_y/∂x) [1/m²]
     * @param gamma  Lorentz gamma of the reference particle
     */
    public static TransferMatrix thickQuadrupole(double length, double k, double gamma) {
        SimpleMatrix m = SimpleMatrix.identity(6);

        if (Math.abs(k) < 1e-12) {
            // k ≈ 0 → pure drift
            m.set(0, 1, length);
            m.set(2, 3, length);
        } else {
            double sqrtK = Math.sqrt(Math.abs(k));
            double phi = sqrtK * length;

            if (k > 0) {
                // Horizontal focusing, vertical defocusing
                m.set(0, 0, Math.cos(phi));
                m.set(0, 1, Math.sin(phi) / sqrtK);
                m.set(1, 0, -sqrtK * Math.sin(phi));
                m.set(1, 1, Math.cos(phi));

                m.set(2, 2, Math.cosh(phi));
                m.set(2, 3, Math.sinh(phi) / sqrtK);
                m.set(3, 2, sqrtK * Math.sinh(phi));
                m.set(3, 3, Math.cosh(phi));
            } else {
                // Horizontal defocusing, vertical focusing
                m.set(0, 0, Math.cosh(phi));
                m.set(0, 1, Math.sinh(phi) / sqrtK);
                m.set(1, 0, sqrtK * Math.sinh(phi));
                m.set(1, 1, Math.cosh(phi));

                m.set(2, 2, Math.cos(phi));
                m.set(2, 3, Math.sin(phi) / sqrtK);
                m.set(3, 2, -sqrtK * Math.sin(phi));
                m.set(3, 3, Math.cos(phi));
            }
        }

        // Longitudinal: drift-like
        m.set(4, 5, length / (gamma * gamma));
        return new TransferMatrix(m);
    }

    /**
     * Sector dipole with bending radius ρ and arc length L.
     * <p>
     * Provides horizontal bending together with dispersion generation.
     * The vertical plane is a simple drift.
     *
     * @param length arc length [m]
     * @param rho    bending radius [m]
     * @param gamma  Lorentz gamma of the reference particle
     */
    public static TransferMatrix sectorDipole(double length, double rho, double gamma) {
        SimpleMatrix m = SimpleMatrix.identity(6);
        double theta = length / rho;

        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);

        // Horizontal plane with dispersion
        m.set(0, 0, cosT);
        m.set(0, 1, rho * sinT);
        m.set(0, 5, rho * (1.0 - cosT));
        m.set(1, 0, -sinT / rho);
        m.set(1, 1, cosT);
        m.set(1, 5, sinT);

        // Vertical plane – pure drift
        m.set(2, 3, length);

        // Longitudinal
        m.set(4, 0, -sinT);
        m.set(4, 1, -rho * (1.0 - cosT));
        m.set(4, 5, -rho * (theta - sinT) + length / (gamma * gamma));

        return new TransferMatrix(m);
    }

    /**
     * Thin RF-cavity kick for longitudinal dynamics.
     *
     * @param voltage         peak RF voltage [GeV]
     * @param harmonicNumber  RF harmonic number h
     * @param syncPhase       synchronous phase φ_s [rad]
     * @param circumference   ring circumference [m]
     * @param energy          beam energy [GeV]
     * @param gamma           Lorentz gamma
     */
    public static TransferMatrix rfCavity(double voltage, double harmonicNumber,
                                          double syncPhase, double circumference,
                                          double energy, double gamma) {
        SimpleMatrix m = SimpleMatrix.identity(6);

        double k56 = (2.0 * Math.PI * harmonicNumber * voltage * Math.cos(syncPhase))
                / (circumference * energy);
        m.set(5, 4, k56);

        return new TransferMatrix(m);
    }

    /**
     * Thin sextupole – returns the identity in the linear approximation.
     * <p>
     * Sextupoles have no effect on the linear optics; their contribution
     * enters only through second-order (chromatic) terms.
     */
    public static TransferMatrix thinSextupole() {
        return identity();
    }

    // ── Instance methods ────────────────────────────────────────────

    /**
     * Applies this transfer matrix to a phase-space vector.
     *
     * @param ps the input phase-space 6-vector
     * @return the transported phase-space vector
     */
    public PhaseSpace apply(PhaseSpace ps) {
        SimpleMatrix v = new SimpleMatrix(6, 1);
        double[] a = ps.toArray();
        for (int i = 0; i < 6; i++) {
            v.set(i, 0, a[i]);
        }
        SimpleMatrix result = matrix.mult(v);
        return new PhaseSpace(
                result.get(0, 0), result.get(1, 0),
                result.get(2, 0), result.get(3, 0),
                result.get(4, 0), result.get(5, 0));
    }

    /**
     * Composes this matrix with another: <b>result = this · other</b>.
     * Equivalent to {@link #multiply(TransferMatrix)} but named to
     * match the plan specification.
     *
     * @param other the matrix to compose with (applied first)
     * @return a new {@code TransferMatrix} representing the combined transport
     */
    public TransferMatrix compose(TransferMatrix other) {
        return multiply(other);
    }

    /**
     * Composes two transfer matrices: <b>M_total = this · other</b>.
     *
     * @param other the matrix to multiply on the right
     * @return a new {@code TransferMatrix} representing the combined transport
     */
    public TransferMatrix multiply(TransferMatrix other) {
        return new TransferMatrix(this.matrix.mult(other.matrix));
    }

    /** Returns the element at the given row and column (0-indexed). */
    public double get(int row, int col) {
        return matrix.get(row, col);
    }

    /** Returns a defensive copy of the underlying 6×6 matrix. */
    public SimpleMatrix getMatrix() {
        return matrix.copy();
    }

    /**
     * Returns the matrix inverse, if it exists.
     *
     * @return a new {@code TransferMatrix} that is the inverse of this one
     */
    public TransferMatrix inverse() {
        return new TransferMatrix(matrix.invert());
    }

    /**
     * Returns the determinant of the 6×6 matrix.
     * For a symplectic matrix this should be exactly 1.
     *
     * @return the determinant
     */
    public double det() {
        return matrix.determinant();
    }

    /**
     * Propagates Courant–Snyder parameters through this transfer matrix using
     * the standard one-pass transport formulae:
     * <pre>
     *   β₂  =  M₁₁² · β₁  − 2·M₁₁·M₁₂ · α₁  + M₁₂² · γ₁
     *   α₂  = −M₁₁·M₂₁ · β₁ + (M₁₁·M₂₂ + M₁₂·M₂₁) · α₁ − M₁₂·M₂₂ · γ₁
     * </pre>
     * and similarly for the vertical plane using (M₃₃, M₃₄, M₄₃, M₄₄).
     * Dispersion is transported as an ordinary coordinate:
     * <pre>
     *   D_x'  = M₀₀·D_x + M₀₁·D'_x + M₀₅
     *   D'_x' = M₁₀·D_x + M₁₁·D'_x + M₁₅
     * </pre>
     *
     * @param in input Twiss parameters
     * @return new Twiss parameters after transport
     */
    public TwissParameters transportTwiss(TwissParameters in) {
        // Horizontal plane
        double m11 = matrix.get(0, 0);
        double m12 = matrix.get(0, 1);
        double m21 = matrix.get(1, 0);
        double m22 = matrix.get(1, 1);

        double betaX1 = in.getBetaX();
        double alphaX1 = in.getAlphaX();
        double gammaX1 = in.gammaX();

        double betaX2 = m11 * m11 * betaX1
                - 2.0 * m11 * m12 * alphaX1
                + m12 * m12 * gammaX1;
        double alphaX2 = -m11 * m21 * betaX1
                + (m11 * m22 + m12 * m21) * alphaX1
                - m12 * m22 * gammaX1;

        // Vertical plane
        double m33 = matrix.get(2, 2);
        double m34 = matrix.get(2, 3);
        double m43 = matrix.get(3, 2);
        double m44 = matrix.get(3, 3);

        double betaY1 = in.getBetaY();
        double alphaY1 = in.getAlphaY();
        double gammaY1 = in.gammaY();

        double betaY2 = m33 * m33 * betaY1
                - 2.0 * m33 * m34 * alphaY1
                + m34 * m34 * gammaY1;
        double alphaY2 = -m33 * m43 * betaY1
                + (m33 * m44 + m34 * m43) * alphaY1
                - m34 * m44 * gammaY1;

        // Dispersion transport
        double newDispX = matrix.get(0, 0) * in.getDispX()
                + matrix.get(0, 1) * in.getDispPX()
                + matrix.get(0, 5);
        double newDispPX = matrix.get(1, 0) * in.getDispX()
                + matrix.get(1, 1) * in.getDispPX()
                + matrix.get(1, 5);

        return new TwissParameters(betaX2, alphaX2, betaY2, alphaY2,
                newDispX, newDispPX);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferMatrix 6×6:\n");
        for (int r = 0; r < 6; r++) {
            sb.append("  [");
            for (int c = 0; c < 6; c++) {
                if (c > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("%12.6f", matrix.get(r, c)));
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
}
