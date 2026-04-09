package com.lhcsim.app;

import com.badlogic.gdx.Game;
import com.lhcsim.core.EventBus;
import com.lhcsim.core.GameState;
import com.lhcsim.core.RandomService;
import com.lhcsim.core.TimeManager;
import com.lhcsim.core.config.AppConfig;
import com.lhcsim.core.config.ConfigLoader;
import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.economy.BeamTimeManager;
import com.lhcsim.physics.collision.CrossSectionTable;
import com.lhcsim.physics.particles.ParticleDatabase;
import com.lhcsim.app.screens.SplashScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level LibGDX {@link Game} class that initialises all core systems.
 * <p>
 * On startup the game transitions: Splash → MainMenu.  The {@link ScreenManager}
 * manages the screen stack.
 */
public class LhcSimGame extends Game {

    private static final Logger LOG = LoggerFactory.getLogger(LhcSimGame.class);

    private EventBus eventBus;
    private RandomService randomService;
    private TimeManager timeManager;
    private GameState gameState;
    private ParticleDatabase particleDatabase;
    private CrossSectionTable crossSectionTable;
    private AlertSystem alertSystem;
    private BeamTimeManager beamTimeManager;
    private ScreenManager screenManager;
    private AppConfig appConfig;

    @Override
    public void create() {
        LOG.info("LHC Simulator starting...");

        // Load config
        appConfig = ConfigLoader.load();

        eventBus = new EventBus();
        randomService = new RandomService(System.nanoTime());
        timeManager = new TimeManager(eventBus);
        gameState = new GameState();

        try {
            particleDatabase = ParticleDatabase.loadDefault();
            crossSectionTable = CrossSectionTable.loadDefault();
        } catch (Exception e) {
            LOG.error("Failed to load data files", e);
            throw new RuntimeException("Failed to load data files", e);
        }

        alertSystem = new AlertSystem(eventBus);
        beamTimeManager = new BeamTimeManager(eventBus);

        LOG.info("Loaded {} particles, {} processes",
                particleDatabase.size(),
                crossSectionTable.getAllProcesses().size());

        screenManager = new ScreenManager(this);
        screenManager.push(new SplashScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        eventBus.clear();
        LOG.info("LHC Simulator shut down.");
    }

    // ── Singleton-style accessors ───────────────────────────────────

    /** Global event bus. */
    public EventBus eventBus()                    { return eventBus; }
    public RandomService getRandomService()       { return randomService; }
    public TimeManager getTimeManager()           { return timeManager; }
    public GameState getGameState()               { return gameState; }
    public ParticleDatabase getParticleDatabase() { return particleDatabase; }
    public CrossSectionTable getCrossSectionTable(){ return crossSectionTable; }
    public AlertSystem getAlertSystem()           { return alertSystem; }
    public BeamTimeManager getBeamTimeManager()   { return beamTimeManager; }
    public ScreenManager getScreenManager()       { return screenManager; }
    public AppConfig getAppConfig()               { return appConfig; }

    // Keep backward compat with existing code that calls getEventBus()
    public EventBus getEventBus()                 { return eventBus; }
}
