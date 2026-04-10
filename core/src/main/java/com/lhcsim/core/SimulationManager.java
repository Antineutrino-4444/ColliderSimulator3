package com.lhcsim.core;

import com.lhcsim.game.campaign.Era;
import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.controlroom.SubsystemStatus;
import com.lhcsim.game.economy.BeamTimeManager;
import com.lhcsim.game.economy.LumiBank;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.physics.beam.TwissParameters;
import com.lhcsim.physics.collision.CrossSectionTable;
import com.lhcsim.physics.collision.EventGenerator;
import com.lhcsim.physics.collision.EventGenerator.PhysicsEvent;
import com.lhcsim.physics.collision.LuminosityCalculator;
import com.lhcsim.physics.detector.DetectorConfig;
import com.lhcsim.physics.detector.FastDetectorSim;
import com.lhcsim.physics.particles.ParticleDatabase;
import com.lhcsim.physics.particles.ReconstructedObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full physics simulation loop: beam setup, luminosity
 * calculation, event generation, detector simulation, subsystem health,
 * alert generation, and beam-time accounting.
 */
public class SimulationManager {

    // ── EventBus event records ──────────────────────────────────────
    public record SimulationTick(double deltaHours, long totalEvents,
                                 double instLumi, double intLumiFb) {}
    public record EventsBatch(List<PhysicsEvent> events,
                              List<List<ReconstructedObject>> recoObjects) {}
    public record EraChanged(Era newEra) {}
    public record BeamStateChanged(boolean beamOn) {}

    // ── LHC / collider parameters ──────────────────────────────────
    public static final double LHC_CIRCUMFERENCE = 26_658.883;
    public static final int NUM_BUNCHES = 2808;
    private static final double CROSSING_ANGLE = 285e-6;
    private static final double BETA_STAR_X = 0.30;
    private static final double BETA_STAR_Y = 0.30;
    private static final double INEL_XSEC_CM2 = 80e-27;

    /**
     * Maximum wall-clock seconds consumed in a single update call.
     * If the frame delta exceeds this, the excess is discarded rather than
     * simulated, preventing a spiral of ever-longer frames.
     */
    private static final float MAX_FRAME_DELTA = 0.05f;

    // ── Dependencies ──────────────────────────────────────────────
    private final EventBus eventBus;
    private final RandomService random;
    private final TimeManager timeManager;
    private final BeamTimeManager beamTimeManager;
    private final AlertSystem alertSystem;
    private final ParticleDatabase particleDb;
    private final CrossSectionTable crossSectionTable;

    // ── Simulation state ──────────────────────────────────────────
    private Era currentEra;
    private int eraIndex;
    private final List<Era> allEras;
    private boolean beamOn;

    private Bunch beam1;
    private Bunch beam2;
    private EventGenerator eventGenerator;
    private FastDetectorSim detectorSim;

    private double instLuminosity;
    private double integratedLumiFb;
    private long totalEvents;
    private long eventCounter;

    private final List<SubsystemStatus> subsystems = new ArrayList<>();
    private final Map<String, Long> eventsByProcess = new LinkedHashMap<>();
    private final LumiBank lumiBank = new LumiBank();

    private double alertCooldown;
    private double subsystemDegradeCooldown;

    // ── Recent events for display ─────────────────────────────────
    private List<PhysicsEvent> lastBatchEvents = List.of();
    private List<List<ReconstructedObject>> lastBatchReco = List.of();

    public SimulationManager(EventBus eventBus, RandomService random,
                             TimeManager timeManager, BeamTimeManager beamTimeManager,
                             AlertSystem alertSystem, ParticleDatabase particleDb,
                             CrossSectionTable crossSectionTable) {
        this.eventBus = eventBus;
        this.random = random;
        this.timeManager = timeManager;
        this.beamTimeManager = beamTimeManager;
        this.alertSystem = alertSystem;
        this.particleDb = particleDb;
        this.crossSectionTable = crossSectionTable;
        this.allEras = Era.createAllEras();
        this.eventGenerator = new EventGenerator(crossSectionTable, particleDb, random);
    }

    /** Start the simulation at era index 0. */
    public void start() {
        eraIndex = 0;
        setEra(allEras.get(eraIndex));
        beamOn = false;

        // Initialise subsystems
        for (String name : SubsystemStatus.SUBSYSTEM_NAMES) {
            subsystems.add(new SubsystemStatus(name));
        }
    }

    /** Advance to the next era, if available. Returns true if advanced. */
    public boolean advanceEra() {
        if (eraIndex + 1 < allEras.size()) {
            eraIndex++;
            setEra(allEras.get(eraIndex));
            return true;
        }
        return false;
    }

    private void setEra(Era era) {
        this.currentEra = era;
        beamTimeManager.setEra(era);

        double beamEnergy = era.sqrtS() * 1000.0 / 2.0; // GeV per beam
        TwissParameters ipTwiss = new TwissParameters(
                BETA_STAR_X, 0, BETA_STAR_Y, 0, 0, 0);
        beam1 = new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.075, 1.13e-4,
                beamEnergy, ipTwiss);
        beam2 = new Bunch(1.15e11, 3.75e-6, 3.75e-6, 0.075, 1.13e-4,
                beamEnergy, ipTwiss);

        DetectorConfig detConfig = DetectorConfig.cms();
        detectorSim = new FastDetectorSim(detConfig, particleDb, random);

        instLuminosity = 0;
        eventBus.post(new EraChanged(era));
    }

    /** Toggle beam on/off. */
    public void toggleBeam() {
        beamOn = !beamOn;
        if (beamOn) {
            instLuminosity = LuminosityCalculator.instantaneousLuminosity(
                    beam1, beam2, NUM_BUNCHES, CROSSING_ANGLE,
                    LHC_CIRCUMFERENCE, BETA_STAR_X, BETA_STAR_Y);
        } else {
            instLuminosity = 0;
        }
        eventBus.post(new BeamStateChanged(beamOn));
    }

    /**
     * Main simulation update, called once per frame.
     *
     * @param realDelta wall-clock seconds since last frame
     */
    public void update(float realDelta) {
        // Cap frame delta to prevent runaway simulation when a frame takes too long
        float cappedDelta = Math.min(realDelta, MAX_FRAME_DELTA);

        // Advance game time
        timeManager.update(cappedDelta);

        // Update alerts
        alertSystem.update(cappedDelta);

        // Clear previous batch so stale events are never re-consumed
        lastBatchEvents = List.of();
        lastBatchReco = List.of();

        if (!beamOn || beamTimeManager.isExhausted()) {
            instLuminosity = 0;
            eventBus.post(new SimulationTick(0, totalEvents, 0, integratedLumiFb));
            return;
        }

        // Compute in-game hours for this tick (timeManager has the speed)
        double speedMultiplier = switch (timeManager.getMode()) {
            case NORMAL   -> TimeManager.NORMAL_SPEED;
            case FAST     -> TimeManager.FAST_SPEED;
            case REALTIME -> 1.0;
            case PAUSED   -> 0.0;
        };
        double deltaHours = cappedDelta * speedMultiplier;

        // Consume beam time
        beamTimeManager.consumeTime(deltaHours);

        // Integrated luminosity: L_int = L_inst * Δt
        double deltaSeconds = deltaHours * 3600.0;
        double deltaLumiCm2 = instLuminosity * deltaSeconds;
        double deltaLumiFb = LuminosityCalculator.toInverseFemtobarns(deltaLumiCm2);
        integratedLumiFb += deltaLumiFb;

        // Accumulate into the per-detector LumiBank (CMS as primary detector)
        lumiBank.accumulate(LumiBank.CMS, instLuminosity, deltaSeconds);

        // Convert to pb for event generation (1 fb = 1000 pb)
        double deltaLumiPb = deltaLumiFb * 1000.0;

        // Generate events — cross-section table is keyed in TeV
        double sqrtSTeV = currentEra.sqrtS();
        List<PhysicsEvent> events = eventGenerator.generateEvents(
                sqrtSTeV, deltaLumiPb, eventCounter);

        List<List<ReconstructedObject>> allReco = new ArrayList<>();
        for (PhysicsEvent ev : events) {
            List<ReconstructedObject> reco = detectorSim.reconstruct(ev);
            allReco.add(reco);
            eventsByProcess.merge(ev.processName(), 1L, Long::sum);
        }

        eventCounter += events.size();
        totalEvents += events.size();
        lastBatchEvents = events;
        lastBatchReco = allReco;

        if (!events.isEmpty()) {
            eventBus.post(new EventsBatch(events, allReco));
        }

        // Subsystem degradation (random minor fluctuations)
        subsystemDegradeCooldown -= realDelta;
        if (subsystemDegradeCooldown <= 0) {
            subsystemDegradeCooldown = 2.0 + random.nextDouble(RandomService.ALERTS) * 3.0;
            degradeRandomSubsystem();
        }

        // Periodic alerts
        alertCooldown -= realDelta;
        if (alertCooldown <= 0) {
            alertCooldown = 15.0 + random.nextDouble(RandomService.ALERTS) * 30.0;
            generateRandomAlert();
        }

        eventBus.post(new SimulationTick(deltaHours, totalEvents,
                instLuminosity, integratedLumiFb));
    }

    private void degradeRandomSubsystem() {
        if (subsystems.isEmpty()) return;
        int idx = random.nextInt(RandomService.ALERTS, subsystems.size());
        SubsystemStatus ss = subsystems.get(idx);
        double delta = -0.5 - random.nextDouble(RandomService.ALERTS) * 2.0;
        ss.setHealthPercent(ss.getHealthPercent() + delta);

        if (ss.getHealthPercent() < 40) {
            ss.setStatusMessage("Degraded performance");
        }
    }

    private void generateRandomAlert() {
        String[] messages = {
                "Cryogenics temperature rising in sector 3-4",
                "Vacuum spike detected in beam pipe",
                "Magnet quench warning in cell 14L2",
                "RF frequency drift detected",
                "Collimator jaw misalignment",
                "Beam loss monitor threshold exceeded",
                "Trigger rate above nominal",
                "DAQ buffer overflow warning"
        };
        AlertSystem.AlertSeverity[] severities = {
                AlertSystem.AlertSeverity.INFO,
                AlertSystem.AlertSeverity.WARNING,
                AlertSystem.AlertSeverity.INFO,
                AlertSystem.AlertSeverity.WARNING,
                AlertSystem.AlertSeverity.INFO,
                AlertSystem.AlertSeverity.CRITICAL,
                AlertSystem.AlertSeverity.WARNING,
                AlertSystem.AlertSeverity.WARNING
        };
        int idx = random.nextInt(RandomService.ALERTS, messages.length);
        String subsystem = SubsystemStatus.SUBSYSTEM_NAMES.get(
                idx % SubsystemStatus.SUBSYSTEM_NAMES.size());
        alertSystem.createAlert(subsystem, messages[idx], severities[idx], 20.0);
    }

    /** Repair a subsystem back towards 100%. */
    public void repairSubsystem(int index) {
        if (index >= 0 && index < subsystems.size()) {
            SubsystemStatus ss = subsystems.get(index);
            ss.setHealthPercent(Math.min(100, ss.getHealthPercent() + 25));
            ss.setStatusMessage("All systems nominal");
        }
    }

    // ── Getters ─────────────────────────────────────────────────────

    public Era getCurrentEra()                { return currentEra; }
    public int getEraIndex()                  { return eraIndex; }
    public List<Era> getAllEras()              { return allEras; }
    public boolean isBeamOn()                 { return beamOn; }
    public double getInstLuminosity()         { return instLuminosity; }
    public double getIntegratedLumiFb()       { return integratedLumiFb; }
    public long getTotalEvents()              { return totalEvents; }
    public List<SubsystemStatus> getSubsystems() { return subsystems; }
    public Map<String, Long> getEventsByProcess() { return eventsByProcess; }
    public List<PhysicsEvent> getLastBatchEvents() { return lastBatchEvents; }
    public List<List<ReconstructedObject>> getLastBatchReco() { return lastBatchReco; }
    public Bunch getBeam1()                   { return beam1; }
    public Bunch getBeam2()                   { return beam2; }
    public FastDetectorSim getDetectorSim()   { return detectorSim; }
    public LumiBank getLumiBank()             { return lumiBank; }
}
