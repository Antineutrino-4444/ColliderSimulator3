package com.lhcsim.physics.particles;

import com.lhcsim.core.RngStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Recursive decay engine using PDG branching ratios with proper Lorentz kinematics.
 * <p>
 * Decays unstable particles into daughters, recursing until all products are stable.
 * Uses exact two-body kinematics for two-body decays and flat phase-space sampling
 * (Raubold-Lynch-like) for three-body and higher.
 * <p>
 * Stable particles are defined as those in the {@link #STABLE_PDGS} set:
 * e, µ, γ, p, n, ν (all flavors), π±, K±, K_L.
 */
public class DecayEngine {

    /**
     * PDG IDs of particles considered stable for this simulation.
     * Includes antiparticles.
     */
    public static final Set<Integer> STABLE_PDGS = Set.of(
            11, -11,   // electron, positron
            13, -13,   // muon, antimuon
            22,        // photon
            2212, -2212, // proton, antiproton
            2112, -2112, // neutron, antineutron
            12, -12,   // nu_e
            14, -14,   // nu_mu
            16, -16,   // nu_tau
            211, -211, // pi+, pi-
            321, -321, // K+, K-
            130        // K_L
    );

    private static final int MAX_RECURSION = 20;

    private final ParticleDatabase particleDb;
    private final RngStream rng;

    public DecayEngine(ParticleDatabase particleDb, RngStream rng) {
        this.particleDb = particleDb;
        this.rng = rng;
    }

    /**
     * A stable final-state particle with its four-momentum and identity.
     */
    public record StableParticle(int pdgId, FourMomentum momentum) {}

    /**
     * Decays a particle and returns all stable products.
     *
     * @param pdgId    PDG ID of the parent particle
     * @param momentum four-momentum of the parent in the lab frame
     * @return list of stable final-state particles
     */
    public List<StableParticle> decay(int pdgId, FourMomentum momentum) {
        return decayRecursive(pdgId, momentum, 0);
    }

    private List<StableParticle> decayRecursive(int pdgId, FourMomentum momentum, int depth) {
        List<StableParticle> result = new ArrayList<>();

        // Check if stable
        if (isStable(pdgId) || depth >= MAX_RECURSION) {
            result.add(new StableParticle(pdgId, momentum));
            return result;
        }

        // Look up decay channels
        Particle particle = particleDb.getParticleByPdgId(pdgId);
        if (particle == null || particle.decays() == null || particle.decays().isEmpty()) {
            // Unknown particle or no decays: treat as stable
            result.add(new StableParticle(pdgId, momentum));
            return result;
        }

        // Select decay channel by branching ratio
        DecayChannel channel = selectChannel(particle.decays());
        List<Integer> products = channel.getProducts();

        if (products.isEmpty()) {
            result.add(new StableParticle(pdgId, momentum));
            return result;
        }

        // Generate daughter momenta
        double parentMass = momentum.invariantMass();
        List<FourMomentum> daughterMomenta;

        if (products.size() == 2) {
            daughterMomenta = twoBodyDecay(parentMass, products);
        } else {
            daughterMomenta = multiBodyDecay(parentMass, products);
        }

        // Boost daughters from parent rest frame to lab frame
        LorentzBoost toLab = LorentzBoost.fromFourMomentum(momentum).inverse();

        for (int i = 0; i < products.size(); i++) {
            FourMomentum boosted = toLab.apply(daughterMomenta.get(i));
            result.addAll(decayRecursive(products.get(i), boosted, depth + 1));
        }

        return result;
    }

    /**
     * Checks if a particle is considered stable.
     */
    public boolean isStable(int pdgId) {
        return STABLE_PDGS.contains(pdgId);
    }

    /**
     * Selects a decay channel based on branching ratios.
     */
    private DecayChannel selectChannel(List<DecayChannel> channels) {
        double r = rng.nextDouble();
        double cumulative = 0;
        for (DecayChannel ch : channels) {
            cumulative += ch.getBranchingRatio();
            if (r < cumulative) {
                return ch;
            }
        }
        return channels.get(channels.size() - 1); // fallback
    }

    /**
     * Exact two-body decay in the parent rest frame.
     */
    private List<FourMomentum> twoBodyDecay(double parentMass, List<Integer> products) {
        double m1 = getMass(products.get(0));
        double m2 = getMass(products.get(1));

        // CM momentum magnitude
        double pStar = twoBodyMomentum(parentMass, m1, m2);

        // Isotropic direction
        double cosTheta = 2.0 * rng.nextDouble() - 1.0;
        double sinTheta = Math.sqrt(Math.max(0, 1.0 - cosTheta * cosTheta));
        double phi = 2.0 * Math.PI * rng.nextDouble();

        double px = pStar * sinTheta * Math.cos(phi);
        double py = pStar * sinTheta * Math.sin(phi);
        double pz = pStar * cosTheta;

        double e1 = Math.sqrt(pStar * pStar + m1 * m1);
        double e2 = Math.sqrt(pStar * pStar + m2 * m2);

        List<FourMomentum> result = new ArrayList<>(2);
        result.add(new FourMomentum(e1, px, py, pz));
        result.add(new FourMomentum(e2, -px, -py, -pz));
        return result;
    }

    /**
     * Multi-body (≥3) phase-space decay in the parent rest frame.
     * Uses a simplified Raubold-Lynch algorithm with sequential two-body decays.
     */
    private List<FourMomentum> multiBodyDecay(double parentMass, List<Integer> products) {
        int n = products.size();
        double[] masses = new double[n];
        double totalChildMass = 0;
        for (int i = 0; i < n; i++) {
            masses[i] = getMass(products.get(i));
            totalChildMass += masses[i];
        }

        if (parentMass < totalChildMass) {
            // Not enough energy: give particles at rest
            List<FourMomentum> result = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                result.add(FourMomentum.atRest(masses[i]));
            }
            return result;
        }

        // Sequential two-body decay approach:
        // Split into particle[0] + composite(1..n-1), then recurse
        // Sample invariant mass of the composite system
        List<FourMomentum> result = new ArrayList<>(n);
        sequentialDecay(parentMass, masses, 0, result);
        return result;
    }

    private void sequentialDecay(double availableMass, double[] masses, int start,
                                  List<FourMomentum> result) {
        int remaining = masses.length - start;
        if (remaining == 1) {
            // Last particle gets whatever is left
            result.add(FourMomentum.atRest(masses[start]));
            return;
        }
        if (remaining == 2) {
            // Direct two-body decay
            double pStar = twoBodyMomentum(availableMass, masses[start], masses[start + 1]);
            double cosTheta = 2.0 * rng.nextDouble() - 1.0;
            double sinTheta = Math.sqrt(Math.max(0, 1.0 - cosTheta * cosTheta));
            double phi = 2.0 * Math.PI * rng.nextDouble();

            double px = pStar * sinTheta * Math.cos(phi);
            double py = pStar * sinTheta * Math.sin(phi);
            double pz = pStar * cosTheta;

            double e1 = Math.sqrt(pStar * pStar + masses[start] * masses[start]);
            double e2 = Math.sqrt(pStar * pStar + masses[start + 1] * masses[start + 1]);

            result.add(new FourMomentum(e1, px, py, pz));
            result.add(new FourMomentum(e2, -px, -py, -pz));
            return;
        }

        // Sample the invariant mass of the (start+1..end) composite
        double minCompositeMass = 0;
        for (int i = start + 1; i < masses.length; i++) {
            minCompositeMass += masses[i];
        }
        double maxCompositeMass = availableMass - masses[start];

        // Sample uniformly in the allowed range
        double compositeMass = minCompositeMass
                + rng.nextDouble() * (maxCompositeMass - minCompositeMass);

        // Two-body decay: particle[start] + composite
        double pStar = twoBodyMomentum(availableMass, masses[start], compositeMass);
        double cosTheta = 2.0 * rng.nextDouble() - 1.0;
        double sinTheta = Math.sqrt(Math.max(0, 1.0 - cosTheta * cosTheta));
        double phi = 2.0 * Math.PI * rng.nextDouble();

        double px = pStar * sinTheta * Math.cos(phi);
        double py = pStar * sinTheta * Math.sin(phi);
        double pz = pStar * cosTheta;

        double e1 = Math.sqrt(pStar * pStar + masses[start] * masses[start]);
        result.add(new FourMomentum(e1, px, py, pz));

        // Recurse on the composite system: generate in its rest frame, then boost
        double eComposite = Math.sqrt(pStar * pStar + compositeMass * compositeMass);
        FourMomentum compositeP = new FourMomentum(eComposite, -px, -py, -pz);

        // Generate children in composite rest frame
        List<FourMomentum> subProducts = new ArrayList<>();
        sequentialDecay(compositeMass, masses, start + 1, subProducts);

        // Boost back to the current frame (inverse of toRest = toLab)
        LorentzBoost toCurrentFrame = LorentzBoost.fromFourMomentum(compositeP).inverse();
        for (FourMomentum sp : subProducts) {
            result.add(toCurrentFrame.apply(sp));
        }
    }

    /**
     * Two-body CM momentum: p* = λ^{1/2}(M², m1², m2²) / (2M)
     * where λ(a,b,c) = a² + b² + c² - 2ab - 2ac - 2bc.
     */
    private double twoBodyMomentum(double M, double m1, double m2) {
        double M2 = M * M;
        double m12 = m1 * m1;
        double m22 = m2 * m2;
        double lambda = M2 * M2 + m12 * m12 + m22 * m22
                - 2 * M2 * m12 - 2 * M2 * m22 - 2 * m12 * m22;
        return lambda > 0 ? Math.sqrt(lambda) / (2 * M) : 0;
    }

    private double getMass(int pdgId) {
        Particle p = particleDb.getParticleByPdgId(pdgId);
        if (p != null) return p.massGeV();
        ParticleData pd = particleDb.getByPdgId(pdgId);
        if (pd != null) return pd.getMass();
        return 0; // massless default
    }
}
