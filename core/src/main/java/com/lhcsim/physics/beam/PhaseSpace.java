package com.lhcsim.physics.beam;

/**
 * Six-dimensional phase-space vector for a single particle.
 * <p>
 * Components:
 * <ul>
 *   <li>{@code x}  — horizontal position [m]</li>
 *   <li>{@code xp} — horizontal angle x' = dx/ds [rad]</li>
 *   <li>{@code y}  — vertical position [m]</li>
 *   <li>{@code yp} — vertical angle y' = dy/ds [rad]</li>
 *   <li>{@code z}  — longitudinal offset from reference [m]</li>
 *   <li>{@code dp}  — relative momentum deviation delta = Delta_p / p_0</li>
 * </ul>
 *
 * @param x  horizontal position [m]
 * @param xp horizontal angle [rad]
 * @param y  vertical position [m]
 * @param yp vertical angle [rad]
 * @param z  longitudinal offset [m]
 * @param dp relative momentum deviation Delta_p/p_0
 */
public record PhaseSpace(double x, double xp, double y, double yp, double z, double dp) {

    /** Returns the phase-space origin (all zeroes). */
    public static PhaseSpace origin() {
        return new PhaseSpace(0, 0, 0, 0, 0, 0);
    }

    /** Returns the components as a 6-element array. */
    public double[] toArray() {
        return new double[]{x, xp, y, yp, z, dp};
    }

    /** Creates a PhaseSpace from a 6-element array. */
    public static PhaseSpace fromArray(double[] a) {
        if (a.length != 6) {
            throw new IllegalArgumentException("Array must have exactly 6 elements");
        }
        return new PhaseSpace(a[0], a[1], a[2], a[3], a[4], a[5]);
    }
}
