package com.lhcsim.physics.detector;

import com.lhcsim.core.RandomService;
import com.lhcsim.physics.collision.EventGenerator.GeneratedParticle;
import com.lhcsim.physics.collision.EventGenerator.PhysicsEvent;
import com.lhcsim.physics.particles.ParticleData;
import com.lhcsim.physics.particles.ParticleDatabase;
import com.lhcsim.physics.particles.ReconstructedObject;
import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;
import com.lhcsim.physics.particles.ReconstructedObject.Quality;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast parametric detector simulation.
 * <p>
 * For each final-state particle in a {@link PhysicsEvent} the simulator
 * checks geometric acceptance (|η|, pT thresholds), classifies the particle
 * type, and applies Gaussian energy/momentum smearing according to the
 * configured detector resolution.
 */
public class FastDetectorSim {

    private final DetectorConfig config;
    private final ParticleDatabase particleDb;
    private final RandomService random;

    public FastDetectorSim(DetectorConfig config, ParticleDatabase particleDb,
                           RandomService random) {
        this.config = config;
        this.particleDb = particleDb;
        this.random = random;
    }

    /**
     * Reconstructs detector-level objects from a generated physics event.
     *
     * @param event generator-level event
     * @return list of smeared reconstructed objects that pass acceptance cuts
     */
    public List<ReconstructedObject> reconstruct(PhysicsEvent event) {
        List<ReconstructedObject> result = new ArrayList<>();

        for (GeneratedParticle gp : event.particles()) {
            if (!gp.isFinalState()) {
                continue;
            }

            double px = gp.px();
            double py = gp.py();
            double pz = gp.pz();
            double energy = gp.energy();

            double pT = Math.sqrt(px * px + py * py);
            double p = Math.sqrt(px * px + py * py + pz * pz);
            double eta = pseudorapidity(p, pz);
            double phi = Math.atan2(py, px);

            // Acceptance cuts
            if (pT < config.getTrackerPtMin()) {
                continue;
            }

            int absPdg = Math.abs(gp.pdgId());
            ObjectType type = classifyParticle(absPdg);
            if (type == null) {
                continue;
            }

            double etaLimit = (type == ObjectType.MUON)
                    ? config.getMuonEtaMax()
                    : config.getTrackerEtaMax();
            if (Math.abs(eta) > etaLimit) {
                continue;
            }

            // Smearing
            double smearedPt;
            double smearedEnergy;
            switch (type) {
                case ELECTRON, PHOTON -> {
                    double sigma = resolution(config.getEcalResA(), config.getEcalResB(), energy);
                    double smearFactor = 1.0 + random.nextGaussian(RandomService.DETECTOR, 0, sigma);
                    smearedEnergy = energy * Math.max(smearFactor, 0.01);
                    smearedPt = pT * Math.max(smearFactor, 0.01);
                }
                case JET, BJET, TAU -> {
                    double sigma = resolution(config.getHcalResA(), config.getHcalResB(), energy);
                    double smearFactor = 1.0 + random.nextGaussian(RandomService.DETECTOR, 0, sigma);
                    smearedEnergy = energy * Math.max(smearFactor, 0.01);
                    smearedPt = pT * Math.max(smearFactor, 0.01);
                }
                case MUON -> {
                    double sigma = resolution(config.getTrackerResA(), config.getTrackerResB(), pT);
                    double smearFactor = 1.0 + random.nextGaussian(RandomService.DETECTOR, 0, sigma);
                    smearedPt = pT * Math.max(smearFactor, 0.01);
                    smearedEnergy = energy * Math.max(smearFactor, 0.01);
                }
                default -> {
                    smearedPt = pT;
                    smearedEnergy = energy;
                }
            }

            int charge = determineCharge(gp.pdgId());
            Quality quality = assignQuality();

            result.add(new ReconstructedObject(eta, phi, smearedPt, smearedEnergy,
                    type, quality, charge));
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static double pseudorapidity(double p, double pz) {
        double diff = p - Math.abs(pz);
        if (diff < 1e-10) {
            return pz >= 0 ? 10.0 : -10.0;
        }
        return 0.5 * Math.log((p + pz) / (p - pz));
    }

    /**
     * σ/E = A / √E ⊕ B  →  σ = E · √(A²/E + B²)
     */
    private static double resolution(double a, double b, double e) {
        if (e <= 0) {
            return 0;
        }
        return Math.sqrt(a * a / e + b * b);
    }

    private ObjectType classifyParticle(int absPdg) {
        return switch (absPdg) {
            case 11 -> ObjectType.ELECTRON;
            case 13 -> ObjectType.MUON;
            case 15 -> ObjectType.TAU;
            case 22 -> ObjectType.PHOTON;
            case 5 -> ObjectType.BJET;
            case 12, 14, 16 -> ObjectType.MET;
            case 1, 2, 3, 4, 6, 21 -> ObjectType.JET;
            default -> {
                ParticleData pd = particleDb.getByPdgId(absPdg);
                if (pd != null && "baryon".equals(pd.getType())) {
                    yield ObjectType.JET;
                }
                yield null;
            }
        };
    }

    private int determineCharge(int pdgId) {
        ParticleData pd = particleDb.getByPdgId(pdgId);
        if (pd != null) {
            return (int) Math.signum(pd.getCharge());
        }
        return 0;
    }

    private Quality assignQuality() {
        double roll = random.nextDouble(RandomService.DETECTOR);
        if (roll < 0.6) return Quality.TIGHT;
        if (roll < 0.9) return Quality.MEDIUM;
        return Quality.LOOSE;
    }

    public DetectorConfig getConfig() {
        return config;
    }
}
