package com.lhcsim.physics.collision.matrix;

import com.lhcsim.physics.particles.FourMomentum;

/**
 * Interface for leading-order 2→2 matrix element calculations.
 * <p>
 * Implementations provide |M|² for specific processes, which is used
 * in cross-section calculations and event weight computation.
 */
public interface MatrixElement {

    /**
     * Computes the spin- and color-averaged squared matrix element |M|²
     * for a 2→2 process.
     *
     * @param p1 incoming parton 1
     * @param p2 incoming parton 2
     * @param p3 outgoing particle 1
     * @param p4 outgoing particle 2
     * @return |M|² (dimensionless in natural units, or GeV^-2 depending on process)
     */
    double squared(FourMomentum p1, FourMomentum p2, FourMomentum p3, FourMomentum p4);

    /**
     * Returns the name of the process.
     */
    String processName();
}
