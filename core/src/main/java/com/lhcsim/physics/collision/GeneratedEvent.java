package com.lhcsim.physics.collision;

import com.lhcsim.physics.particles.DecayEngine;
import com.lhcsim.physics.particles.FourMomentum;

import java.util.List;

/**
 * A fully generated collision event including final-state particles
 * from the Pythia-lite pipeline (PDF → matrix element → shower → hadronize → decay).
 *
 * @param process    the hard-scatter process type
 * @param ecm        centre-of-mass energy [GeV]
 * @param hardSystem four-momentum of the hard-scatter system
 * @param finalState list of stable final-state particles
 * @param nPileup    number of pileup interactions in this bunch crossing
 */
public record GeneratedEvent(
        Process process,
        double ecm,
        FourMomentum hardSystem,
        List<DecayEngine.StableParticle> finalState,
        int nPileup
) {}
