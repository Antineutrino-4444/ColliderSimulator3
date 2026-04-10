package com.lhcsim.physics.particles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory particle database loaded from a JSON resource.
 * <p>
 * Particles are indexed by PDG ID and by case-insensitive name for fast lookup.
 * The default database is accessible as a singleton via {@link #getInstance()}.
 * <p>
 * On load, validates that for every particle with decay channels the
 * branching ratios sum to 1.0 within a tolerance of 1e-3.
 * <p>
 * Reference: PDG 2024 — <a href="https://pdg.lbl.gov/">pdg.lbl.gov</a>.
 */
public class ParticleDatabase {

    private static final Logger log = LoggerFactory.getLogger(ParticleDatabase.class);
    private static final String DEFAULT_RESOURCE = "/data/particles.json";

    private static volatile ParticleDatabase instance;

    private final Map<Integer, ParticleData> byPdgId = new LinkedHashMap<>();
    private final Map<String, ParticleData> byName = new LinkedHashMap<>();
    private final Map<Integer, Particle> particleByPdgId = new LinkedHashMap<>();
    private final Map<String, Particle> particleByName = new LinkedHashMap<>();

    private ParticleDatabase() {
    }

    /**
     * Returns the singleton instance, loading from the default classpath
     * resource on first call.
     *
     * @return the shared {@code ParticleDatabase}
     */
    public static ParticleDatabase getInstance() {
        if (instance == null) {
            synchronized (ParticleDatabase.class) {
                if (instance == null) {
                    try {
                        instance = loadDefault();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load default particle database", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Loads the particle database from the given input stream.
     *
     * @param inputStream JSON input containing a list of {@link ParticleData} entries
     * @return a fully-populated {@code ParticleDatabase}
     * @throws IOException if the stream cannot be read or parsed
     */
    public static ParticleDatabase load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<ParticleData> particles = mapper.readValue(
                inputStream, new TypeReference<List<ParticleData>>() {});
        ParticleDatabase db = new ParticleDatabase();
        for (ParticleData p : particles) {
            db.byPdgId.put(p.getPdgId(), p);
            if (p.getName() != null) {
                db.byName.put(p.getName().toLowerCase(), p);
            }
            // Also populate the Particle (record) index
            Particle rec = new Particle(
                    p.getPdgId(), p.getName(), p.getSymbol(),
                    p.getMass(), p.getWidth(), p.getCharge(),
                    p.getSpin(), 0, p.getDecayChannels());
            db.particleByPdgId.put(rec.pdgId(), rec);
            if (rec.name() != null) {
                db.particleByName.put(rec.name().toLowerCase(), rec);
            }
        }
        db.validateBranchingRatios();
        return db;
    }

    /**
     * Loads the default particle database bundled as a classpath resource
     * at {@value #DEFAULT_RESOURCE}.
     *
     * @return a fully-populated {@code ParticleDatabase}
     * @throws IOException if the resource is missing or cannot be parsed
     */
    public static ParticleDatabase loadDefault() throws IOException {
        try (InputStream is = ParticleDatabase.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (is == null) {
                throw new IOException("Default particle database not found: " + DEFAULT_RESOURCE);
            }
            return load(is);
        }
    }

    // ── Lookup by ParticleData (legacy API) ─────────────────────────

    /**
     * Looks up a particle by its PDG Monte-Carlo ID.
     *
     * @param pdgId the PDG identifier
     * @return the particle data, or {@code null} if not found
     */
    public ParticleData getByPdgId(int pdgId) {
        return byPdgId.get(pdgId);
    }

    /**
     * Looks up a particle by name (case-insensitive).
     *
     * @param name the particle name (e.g. "Higgs", "muon")
     * @return the particle data, or {@code null} if not found
     */
    public ParticleData getByName(String name) {
        if (name == null) {
            return null;
        }
        return byName.get(name.toLowerCase());
    }

    /**
     * Returns an unmodifiable collection of all particles in the database.
     */
    public Collection<ParticleData> getAllParticles() {
        return Collections.unmodifiableCollection(byPdgId.values());
    }

    // ── Lookup by Particle record (new API) ─────────────────────────

    /**
     * Looks up a {@link Particle} record by PDG ID.
     *
     * @param pdgId the PDG identifier
     * @return the particle record, or {@code null} if not found
     */
    public Particle getParticleByPdgId(int pdgId) {
        return particleByPdgId.get(pdgId);
    }

    /**
     * Looks up a {@link Particle} record by name (case-insensitive).
     *
     * @param name the particle name
     * @return the particle record, or {@code null} if not found
     */
    public Particle getParticleByName(String name) {
        if (name == null) {
            return null;
        }
        return particleByName.get(name.toLowerCase());
    }

    /**
     * Returns an unmodifiable collection of all {@link Particle} records.
     */
    public Collection<Particle> getAllParticleRecords() {
        return Collections.unmodifiableCollection(particleByPdgId.values());
    }

    /**
     * Returns the number of particles in the database.
     */
    public int size() {
        return byPdgId.size();
    }

    // ── Validation ──────────────────────────────────────────────────

    private void validateBranchingRatios() {
        for (ParticleData p : byPdgId.values()) {
            List<DecayChannel> decays = p.getDecayChannels();
            if (decays == null || decays.isEmpty()) {
                continue;
            }
            double sum = decays.stream()
                    .mapToDouble(DecayChannel::getBranchingRatio)
                    .sum();
            if (Math.abs(sum - 1.0) > 1e-3) {
                log.warn("Branching ratios for {} (PDG {}) sum to {} (expected 1.0)",
                        p.getName(), p.getPdgId(), sum);
            }
        }
    }
}
