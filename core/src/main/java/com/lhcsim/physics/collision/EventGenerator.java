package com.lhcsim.physics.collision;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.particles.ParticleDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simplified Monte-Carlo event generator.
 * <p>
 * Draws the number of events for each physics process from a Poisson
 * distribution and generates a hard-scatter final state using
 * process-specific kinematics.
 */
public class EventGenerator {

    private static final double HIGGS_MASS = 125.25;
    private static final double TOP_MASS = 172.69;
    private static final double W_MASS = 80.377;
    private static final double Z_MASS = 91.1876;

    /** Maximum events generated per process per tick to prevent runaway loops. */
    private static final int MAX_EVENTS_PER_TICK = 50;

    private final CrossSectionTable crossSectionTable;
    private final ParticleDatabase particleDb;
    private final RandomService random;

    public EventGenerator(CrossSectionTable crossSectionTable,
                          ParticleDatabase particleDb,
                          RandomService random) {
        this.crossSectionTable = crossSectionTable;
        this.particleDb = particleDb;
        this.random = random;
    }

    /**
     * Generates physics events for a luminosity slice.
     * <p>
     * The {@code total_inelastic} process is excluded from per-event
     * generation because its enormous cross-section (~80 mb) is used only
     * for pileup calculations, not for generating individual hard events.
     *
     * @param sqrtS        centre-of-mass energy [TeV]
     * @param deltaLumiPb  integrated luminosity delivered in this slice [pb⁻¹]
     * @param eventCounter starting event number; incremented per event
     * @return list of generated physics events
     */
    public List<PhysicsEvent> generateEvents(double sqrtS, double deltaLumiPb,
                                              long eventCounter) {
        List<PhysicsEvent> events = new ArrayList<>();
        Map<String, CrossSectionTable.ProcessCrossSection> allProcs =
                crossSectionTable.getAllProcesses();

        for (var entry : allProcs.entrySet()) {
            String procName = entry.getKey();
            // total_inelastic is a bulk pileup rate, not an individual-event process
            if ("total_inelastic".equals(procName)) {
                continue;
            }
            double sigma = crossSectionTable.getCrossSectionAt(procName, sqrtS);
            double expected = sigma * deltaLumiPb;
            int nEvents = random.nextPoisson(RandomService.EVENTS, expected);

            // Safety cap: never generate more than MAX_EVENTS_PER_TICK per process
            nEvents = Math.min(nEvents, MAX_EVENTS_PER_TICK);

            for (int i = 0; i < nEvents; i++) {
                List<GeneratedParticle> particles = generateHardEvent(procName, sqrtS);
                events.add(new PhysicsEvent(procName, sqrtS, eventCounter++, particles));
            }
        }
        return events;
    }

    /**
     * Generates the final-state particles for a single hard event.
     */
    private List<GeneratedParticle> generateHardEvent(String processName, double sqrtS) {
        return switch (processName) {
            case "higgs_ggf" -> generateHiggsEvent(sqrtS);
            case "ttbar" -> generateTopPairEvent(sqrtS);
            case "w_production" -> generateWEvent(sqrtS);
            case "z_production" -> generateZEvent(sqrtS);
            case "total_inelastic" -> generateDijetEvent(sqrtS);
            default -> generateGenericEvent(processName, sqrtS);
        };
    }

    private List<GeneratedParticle> generateHiggsEvent(double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();
        double mass = HIGGS_MASS;
        double pt = Math.abs(random.nextGaussian(RandomService.EVENTS, 0, mass / 4.0));
        double rapidity = random.nextGaussian(RandomService.EVENTS, 0, 2.0);
        double phi = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;

        double mt = Math.sqrt(mass * mass + pt * pt);
        double px = pt * Math.cos(phi);
        double py = pt * Math.sin(phi);
        double pz = mt * Math.sinh(rapidity);
        double energy = mt * Math.cosh(rapidity);

        particles.add(new GeneratedParticle(25, px, py, pz, energy, false));

        // Higgs → bb̄ (dominant decay)
        addTwoBodyDecay(particles, mass, energy, px, py, pz, 5, -5, 4.18);
        return particles;
    }

    private List<GeneratedParticle> generateTopPairEvent(double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();
        double pairMass = 2 * TOP_MASS + Math.abs(random.nextGaussian(RandomService.EVENTS, 0, 50));
        double rapidity = random.nextGaussian(RandomService.EVENTS, 0, 1.5);
        double phi = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;

        double pt = Math.abs(random.nextGaussian(RandomService.EVENTS, 0, pairMass / 6.0));
        double mt = Math.sqrt(pairMass * pairMass + pt * pt);
        double pxSys = pt * Math.cos(phi);
        double pySys = pt * Math.sin(phi);
        double pzSys = mt * Math.sinh(rapidity);
        double eSys = mt * Math.cosh(rapidity);

        particles.add(new GeneratedParticle(6, pxSys / 2, pySys / 2, pzSys / 2, eSys / 2, false));
        particles.add(new GeneratedParticle(-6, pxSys / 2, pySys / 2, pzSys / 2, eSys / 2, false));
        return particles;
    }

    private List<GeneratedParticle> generateWEvent(double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();
        int sign = random.nextDouble(RandomService.EVENTS) > 0.5 ? 1 : -1;
        int pdgId = sign * 24;

        double pt = Math.abs(random.nextGaussian(RandomService.EVENTS, 0, W_MASS / 3.0));
        double rapidity = random.nextGaussian(RandomService.EVENTS, 0, 2.5);
        double phi = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;

        double mt = Math.sqrt(W_MASS * W_MASS + pt * pt);
        double px = pt * Math.cos(phi);
        double py = pt * Math.sin(phi);
        double pz = mt * Math.sinh(rapidity);
        double energy = mt * Math.cosh(rapidity);

        particles.add(new GeneratedParticle(pdgId, px, py, pz, energy, false));

        // W → lν
        int leptonPdg = sign > 0 ? -11 : 11;
        int neutrinoPdg = sign > 0 ? 12 : -12;
        addTwoBodyDecay(particles, W_MASS, energy, px, py, pz, leptonPdg, neutrinoPdg, 0.0);
        return particles;
    }

    private List<GeneratedParticle> generateZEvent(double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();

        double pt = Math.abs(random.nextGaussian(RandomService.EVENTS, 0, Z_MASS / 3.0));
        double rapidity = random.nextGaussian(RandomService.EVENTS, 0, 2.5);
        double phi = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;

        double mt = Math.sqrt(Z_MASS * Z_MASS + pt * pt);
        double px = pt * Math.cos(phi);
        double py = pt * Math.sin(phi);
        double pz = mt * Math.sinh(rapidity);
        double energy = mt * Math.cosh(rapidity);

        particles.add(new GeneratedParticle(23, px, py, pz, energy, false));

        // Z → ll̄
        addTwoBodyDecay(particles, Z_MASS, energy, px, py, pz, 13, -13, 0.1057);
        return particles;
    }

    private List<GeneratedParticle> generateDijetEvent(double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();
        double ptMin = 20.0;
        double ptMax = sqrtS / 4.0;

        // Power-law spectrum: dσ/dpT ~ pT^(-4)
        double u = random.nextDouble(RandomService.EVENTS);
        double pt = ptMin * Math.pow(ptMax / ptMin, u);

        double eta1 = random.nextGaussian(RandomService.EVENTS, 0, 2.0);
        double eta2 = random.nextGaussian(RandomService.EVENTS, 0, 2.0);
        double phi1 = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;
        double phi2 = phi1 + Math.PI;

        particles.add(buildParticle(21, pt, eta1, phi1, 0.0));
        particles.add(buildParticle(21, pt, eta2, phi2, 0.0));
        return particles;
    }

    private List<GeneratedParticle> generateGenericEvent(String processName, double sqrtS) {
        List<GeneratedParticle> particles = new ArrayList<>();
        double pt = Math.abs(random.nextGaussian(RandomService.EVENTS, 0, sqrtS / 10.0));
        double eta = random.nextGaussian(RandomService.EVENTS, 0, 2.5);
        double phi = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;
        particles.add(buildParticle(21, pt, eta, phi, 0.0));
        return particles;
    }

    private void addTwoBodyDecay(List<GeneratedParticle> particles,
                                 double parentMass, double parentEnergy,
                                 double parentPx, double parentPy, double parentPz,
                                 int pdg1, int pdg2, double childMass) {
        // Simplified: isotropic in parent rest frame then boosted
        double pStar = parentMass / 2.0;
        double cosTheta = 2.0 * random.nextDouble(RandomService.EVENTS) - 1.0;
        double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
        double phiDecay = random.nextDouble(RandomService.EVENTS) * 2.0 * Math.PI;

        double pxStar = pStar * sinTheta * Math.cos(phiDecay);
        double pyStar = pStar * sinTheta * Math.sin(phiDecay);
        double pzStar = pStar * cosTheta;
        double eStar = Math.sqrt(pStar * pStar + childMass * childMass);

        // Approximate boost along parent direction
        double parentP = Math.sqrt(parentPx * parentPx + parentPy * parentPy + parentPz * parentPz);
        double betaBoost = parentP > 0 ? parentP / parentEnergy : 0;
        double gammaBoost = parentP > 0 ? parentEnergy / parentMass : 1.0;

        if (parentP > 0) {
            double nx = parentPx / parentP;
            double ny = parentPy / parentP;
            double nz = parentPz / parentP;
            double pParallel = pxStar * nx + pyStar * ny + pzStar * nz;

            double pParallelBoosted = gammaBoost * (pParallel + betaBoost * eStar);
            double eBoosted = gammaBoost * (eStar + betaBoost * pParallel);

            double dpx = (pParallelBoosted - pParallel) * nx;
            double dpy = (pParallelBoosted - pParallel) * ny;
            double dpz = (pParallelBoosted - pParallel) * nz;

            particles.add(new GeneratedParticle(pdg1,
                    pxStar + dpx, pyStar + dpy, pzStar + dpz, eBoosted, true));
            particles.add(new GeneratedParticle(pdg2,
                    -pxStar - dpx, -pyStar - dpy, -pzStar - dpz,
                    gammaBoost * (eStar - betaBoost * pParallel), true));
        } else {
            particles.add(new GeneratedParticle(pdg1, pxStar, pyStar, pzStar, eStar, true));
            particles.add(new GeneratedParticle(pdg2, -pxStar, -pyStar, -pzStar, eStar, true));
        }
    }

    private GeneratedParticle buildParticle(int pdgId, double pt, double eta,
                                            double phi, double mass) {
        double px = pt * Math.cos(phi);
        double py = pt * Math.sin(phi);
        double pz = pt * Math.sinh(eta);
        double energy = Math.sqrt(px * px + py * py + pz * pz + mass * mass);
        return new GeneratedParticle(pdgId, px, py, pz, energy, true);
    }

    // ── Records ─────────────────────────────────────────────────────

    /**
     * A single generated hard-scatter event.
     */
    public record PhysicsEvent(String processName, double sqrtS,
                               long eventNumber,
                               List<GeneratedParticle> particles) {}

    /**
     * A generated particle with 4-momentum.
     */
    public record GeneratedParticle(int pdgId,
                                    double px, double py, double pz,
                                    double energy,
                                    boolean isFinalState) {}
}
