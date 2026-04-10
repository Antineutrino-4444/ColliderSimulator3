package com.lhcsim.render.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.lhcsim.core.SimulationManager;
import com.lhcsim.core.TimeManager;
import com.lhcsim.game.campaign.Era;
import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.controlroom.SubsystemStatus;
import com.lhcsim.game.economy.BeamTimeManager;
import com.lhcsim.physics.collision.EventGenerator.PhysicsEvent;
import com.lhcsim.physics.particles.ReconstructedObject;
import com.lhcsim.app.LhcSimGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main gameplay screen: the LHC Control Room.
 * <p>
 * Four tabbed views accessed via F1–F4:
 * <ol>
 *   <li><b>Control Room</b> — subsystems, alerts, beam status, luminosity</li>
 *   <li><b>Ring View</b> — top-down LHC schematic with animated beams</li>
 *   <li><b>Event Display</b> — CMS-style detector cross-section with tracks</li>
 *   <li><b>Discovery</b> — invariant-mass histograms and discovery claims</li>
 * </ol>
 * A persistent status bar at top shows era/beam/lumi info; a control bar
 * at the bottom shows keyboard shortcuts.
 */
public class ControlRoomScreen extends ScreenAdapter {

    // ── Design-doc palette ──────────────────────────────────────────
    private static final Color BG_COLOR      = new Color(0x0A / 255f, 0x0E / 255f, 0x1F / 255f, 1f);
    private static final Color PANEL_BG      = new Color(0x10 / 255f, 0x18 / 255f, 0x30 / 255f, 1f);
    private static final Color PANEL_BORDER  = new Color(0x20 / 255f, 0x40 / 255f, 0x70 / 255f, 1f);
    private static final Color ACCENT_CYAN   = new Color(0x00 / 255f, 0xD4 / 255f, 0xFF / 255f, 1f);
    private static final Color ACCENT_GREEN  = new Color(0x30 / 255f, 0xD1 / 255f, 0x58 / 255f, 1f);
    private static final Color ACCENT_YELLOW = new Color(1.0f, 0.85f, 0.2f, 1f);
    private static final Color ACCENT_RED    = new Color(0xFF / 255f, 0x3B / 255f, 0x30 / 255f, 1f);
    private static final Color ACCENT_ORANGE = new Color(0xFF / 255f, 0x6B / 255f, 0x35 / 255f, 1f);
    private static final Color DIM_TEXT      = new Color(0.5f, 0.55f, 0.65f, 1f);
    private static final Color TAB_ACTIVE    = ACCENT_CYAN;
    private static final Color TAB_INACTIVE  = new Color(0.25f, 0.30f, 0.40f, 1f);

    private static final float TOP_BAR_H  = 52;
    private static final float BOT_BAR_H  = 48;
    private static final float TAB_BAR_H  = 28;
    private static final float MARGIN      = 10;

    // ── Tabs ────────────────────────────────────────────────────────
    private enum Tab { CONTROL_ROOM, RING_VIEW, EVENT_DISPLAY, DISCOVERY }
    private static final String[] TAB_LABELS = {
            "F1 Control Room", "F2 Ring View", "F3 Event Display", "F4 Discovery"
    };
    private Tab activeTab = Tab.CONTROL_ROOM;

    // ── Dependencies ────────────────────────────────────────────────
    private final LhcSimGame game;
    private final SimulationManager sim;
    private final TimeManager timeManager;
    private final BeamTimeManager beamTimeManager;
    private final AlertSystem alertSystem;

    // ── Rendering resources ─────────────────────────────────────────
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont headerFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;
    private GlyphLayout layout;

    // ── Sub-renderers ───────────────────────────────────────────────
    private RingViewRenderer ringView;
    private EventDisplayRenderer eventDisplay;
    private DiscoveryRenderer discovery;

    // ── Event log ring buffer ───────────────────────────────────────
    private static final int EVENT_LOG_SIZE = 14;
    private final String[] eventLog = new String[EVENT_LOG_SIZE];
    private int eventLogHead = 0;

    // ── Cached reco objects for event display ───────────────────────
    private List<ReconstructedObject> lastRecoObjects = new ArrayList<>();
    private float animTime;

    public ControlRoomScreen(LhcSimGame game, SimulationManager sim) {
        this.game = game;
        this.sim = sim;
        this.timeManager = game.getTimeManager();
        this.beamTimeManager = game.getBeamTimeManager();
        this.alertSystem = game.getAlertSystem();
    }

    // ── Lifecycle ───────────────────────────────────────────────────

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        layout = new GlyphLayout();

        headerFont = new BitmapFont();
        headerFont.setColor(Color.WHITE);
        headerFont.getData().setScale(1.8f);

        bodyFont = new BitmapFont();
        bodyFont.setColor(Color.WHITE);
        bodyFont.getData().setScale(1.2f);

        smallFont = new BitmapFont();
        smallFont.setColor(DIM_TEXT);
        smallFont.getData().setScale(1.0f);

        ringView = new RingViewRenderer();
        eventDisplay = new EventDisplayRenderer();
        discovery = new DiscoveryRenderer();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return handleKeyDown(keycode);
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return handleClick(screenX, Gdx.graphics.getHeight() - screenY);
            }
        });

        sim.start();
    }

    // ── Input ───────────────────────────────────────────────────────

    private boolean handleKeyDown(int keycode) {
        switch (keycode) {
            // Tab switching
            case Input.Keys.F1 -> activeTab = Tab.CONTROL_ROOM;
            case Input.Keys.F2 -> activeTab = Tab.RING_VIEW;
            case Input.Keys.F3 -> activeTab = Tab.EVENT_DISPLAY;
            case Input.Keys.F4 -> activeTab = Tab.DISCOVERY;

            // Game controls (always active)
            case Input.Keys.SPACE -> sim.toggleBeam();
            case Input.Keys.NUM_1 -> timeManager.setMode(TimeManager.TimeMode.PAUSED);
            case Input.Keys.NUM_2 -> timeManager.setMode(TimeManager.TimeMode.NORMAL);
            case Input.Keys.NUM_3 -> timeManager.setMode(TimeManager.TimeMode.FAST);
            case Input.Keys.N -> sim.advanceEra();
            case Input.Keys.R -> repairWorstSubsystem();
            case Input.Keys.A -> acknowledgeOldestAlert();
            case Input.Keys.D -> {
                if (activeTab == Tab.DISCOVERY && discovery.hasClaimableDiscovery()) {
                    discovery.claimDiscovery();
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private void repairWorstSubsystem() {
        List<SubsystemStatus> subs = sim.getSubsystems();
        int worstIdx = 0;
        double worstHealth = 999;
        for (int i = 0; i < subs.size(); i++) {
            if (subs.get(i).getHealthPercent() < worstHealth) {
                worstHealth = subs.get(i).getHealthPercent();
                worstIdx = i;
            }
        }
        sim.repairSubsystem(worstIdx);
    }

    private void acknowledgeOldestAlert() {
        List<AlertSystem.Alert> alerts = alertSystem.getActiveAlerts();
        if (!alerts.isEmpty()) {
            alertSystem.acknowledgeAlert(alerts.get(0).id());
        }
    }

    private boolean handleClick(int x, int y) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Tab bar click detection
        float tabY = h - TOP_BAR_H - TAB_BAR_H;
        if (y >= tabY && y <= tabY + TAB_BAR_H) {
            float tabW = w / Tab.values().length;
            int tabIdx = (int) (x / tabW);
            if (tabIdx >= 0 && tabIdx < Tab.values().length) {
                activeTab = Tab.values()[tabIdx];
                return true;
            }
        }

        // Beam toggle (bottom-left)
        if (x < w * 0.15f && y < BOT_BAR_H) {
            sim.toggleBeam();
            return true;
        }
        // Time controls (bottom-center)
        if (x > w * 0.3f && x < w * 0.65f && y < BOT_BAR_H) {
            float section = (x - w * 0.3f) / (w * 0.35f);
            if (section < 0.33f) {
                timeManager.setMode(TimeManager.TimeMode.PAUSED);
            } else if (section < 0.66f) {
                timeManager.setMode(TimeManager.TimeMode.NORMAL);
            } else {
                timeManager.setMode(TimeManager.TimeMode.FAST);
            }
            return true;
        }
        // Discovery claim (bottom-right on Discovery tab)
        if (activeTab == Tab.DISCOVERY && x > w * 0.75f && y < BOT_BAR_H) {
            if (discovery.hasClaimableDiscovery()) {
                discovery.claimDiscovery();
            }
            return true;
        }
        return false;
    }

    // ── Render ───────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        animTime += delta;

        // Update simulation (and generate new events) only when not paused
        boolean simRunning = timeManager.getMode() != TimeManager.TimeMode.PAUSED;
        if (simRunning) {
            sim.update(delta);
        }

        // Feed new events to discovery histograms & event display.
        // sim.update() clears lastBatchEvents at the start of each call,
        // so we only see genuinely new events here; when paused the list
        // is empty and nothing is accumulated.
        List<PhysicsEvent> lastEvents = sim.getLastBatchEvents();
        if (!lastEvents.isEmpty()) {
            discovery.accumulateEvents(lastEvents);
            for (PhysicsEvent ev : lastEvents) {
                eventLog[eventLogHead] = String.format("#%d %s  sqrt(s)=%.1f TeV",
                        ev.eventNumber(), ev.processName(), ev.sqrtS());
                eventLogHead = (eventLogHead + 1) % EVENT_LOG_SIZE;
            }
        }
        // Cache latest reco objects for event display
        var lastBatchReco = sim.getLastBatchReco();
        if (lastBatchReco != null && !lastBatchReco.isEmpty()) {
            lastRecoObjects = lastBatchReco.get(lastBatchReco.size() - 1);
        }

        // Clear screen
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, BG_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Content area
        float contentY = BOT_BAR_H + MARGIN;
        float contentH = h - TOP_BAR_H - TAB_BAR_H - BOT_BAR_H - MARGIN * 2;

        // ── Draw top bar + tab bar + bottom bar (always visible) ────
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawChrome(w, h);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        drawChromeBorders(w, h);
        shapes.end();

        batch.begin();
        drawTopBar(w, h);
        drawTabBar(w, h);
        drawBottomBar(w, h);
        batch.end();

        // ── Draw active tab content ────────────────────────────────
        switch (activeTab) {
            case CONTROL_ROOM -> renderControlRoom(w, contentY, contentH, delta);
            case RING_VIEW    -> renderRingView(w, contentY, contentH, delta);
            case EVENT_DISPLAY-> renderEventDisplay(w, contentY, contentH, delta);
            case DISCOVERY    -> renderDiscovery(w, contentY, contentH, delta);
        }
    }

    // ── Chrome (top bar, tab bar, bottom bar) ───────────────────────

    private void drawChrome(float w, float h) {
        // Top bar background
        shapes.setColor(PANEL_BG);
        shapes.rect(0, h - TOP_BAR_H, w, TOP_BAR_H);

        // Tab bar background
        float tabY = h - TOP_BAR_H - TAB_BAR_H;
        shapes.setColor(new Color(0x08 / 255f, 0x0C / 255f, 0x1A / 255f, 1f));
        shapes.rect(0, tabY, w, TAB_BAR_H);

        // Active tab highlight
        float tabW = w / Tab.values().length;
        int idx = activeTab.ordinal();
        shapes.setColor(new Color(ACCENT_CYAN.r, ACCENT_CYAN.g, ACCENT_CYAN.b, 0.15f));
        shapes.rect(idx * tabW, tabY, tabW, TAB_BAR_H);

        // Bottom bar background
        shapes.setColor(PANEL_BG);
        shapes.rect(0, 0, w, BOT_BAR_H);
    }

    private void drawChromeBorders(float w, float h) {
        shapes.setColor(PANEL_BORDER);
        shapes.line(0, h - TOP_BAR_H, w, h - TOP_BAR_H);
        float tabY = h - TOP_BAR_H - TAB_BAR_H;
        shapes.line(0, tabY, w, tabY);
        shapes.line(0, BOT_BAR_H, w, BOT_BAR_H);
        // Active tab underline
        float tabW = w / Tab.values().length;
        int idx = activeTab.ordinal();
        shapes.setColor(ACCENT_CYAN);
        shapes.rectLine(idx * tabW, tabY, (idx + 1) * tabW, tabY, 2f);
    }

    private void drawTopBar(float w, float h) {
        Era era = sim.getCurrentEra();
        float y = h - 10;

        // Title
        headerFont.setColor(ACCENT_CYAN);
        headerFont.draw(batch, "LHC SIMULATOR", MARGIN + 6, y);

        // Era info
        bodyFont.setColor(Color.WHITE);
        String eraText = String.format("Era %d: %s | %s | sqrt(s) = %.1f TeV",
                era.number(), era.name(), era.activeMachine(), era.sqrtS());
        layout.setText(bodyFont, eraText);
        bodyFont.draw(batch, eraText, w * 0.35f, y - 4);

        // Beam status indicator
        Color beamColor = sim.isBeamOn() ? ACCENT_GREEN : ACCENT_RED;
        bodyFont.setColor(beamColor);
        String beamText = sim.isBeamOn() ? "* BEAM ON" : "o BEAM OFF";
        layout.setText(bodyFont, beamText);
        bodyFont.draw(batch, beamText, w - layout.width - MARGIN - 6, y - 4);

        // Second line: luminosity + time
        smallFont.setColor(DIM_TEXT);
        double lumiBankFb = sim.getLumiBank().get("CMS") != null
                ? sim.getLumiBank().get("CMS").totalFb() : sim.getIntegratedLumiFb();
        String line2 = String.format("L=%.2e cm^-2s^-1  Int.L=%.3f fb^-1  Events=%,d  Year %d Day %d [%s]",
                sim.getInstLuminosity(), lumiBankFb,
                sim.getTotalEvents(), timeManager.getYear(), timeManager.getDay(),
                timeManager.getMode().name());
        smallFont.draw(batch, line2, MARGIN + 6, y - 26);
    }

    private void drawTabBar(float w, float h) {
        float tabY = h - TOP_BAR_H - TAB_BAR_H;
        float tabW = w / Tab.values().length;
        for (int i = 0; i < Tab.values().length; i++) {
            boolean active = Tab.values()[i] == activeTab;
            bodyFont.setColor(active ? TAB_ACTIVE : TAB_INACTIVE);
            layout.setText(bodyFont, TAB_LABELS[i]);
            float tx = i * tabW + tabW * 0.5f - layout.width * 0.5f;
            bodyFont.draw(batch, TAB_LABELS[i], tx, tabY + TAB_BAR_H - 6);
        }
    }

    private void drawBottomBar(float w, float h) {
        float y = BOT_BAR_H - 12;

        // Beam toggle
        Color beamBtnC = sim.isBeamOn() ? ACCENT_GREEN : ACCENT_RED;
        bodyFont.setColor(beamBtnC);
        bodyFont.draw(batch, sim.isBeamOn() ? "[SPACE] Stop" : "[SPACE] Start", MARGIN, y);

        // Time controls
        float cx = w * 0.2f;
        TimeManager.TimeMode mode = timeManager.getMode();
        bodyFont.setColor(mode == TimeManager.TimeMode.PAUSED ? ACCENT_YELLOW : DIM_TEXT);
        bodyFont.draw(batch, "[1]||", cx, y);
        cx += 55;
        bodyFont.setColor(mode == TimeManager.TimeMode.NORMAL ? ACCENT_GREEN : DIM_TEXT);
        bodyFont.draw(batch, "[2]>", cx, y);
        cx += 55;
        bodyFont.setColor(mode == TimeManager.TimeMode.FAST ? ACCENT_CYAN : DIM_TEXT);
        bodyFont.draw(batch, "[3]>>", cx, y);

        // Additional controls
        cx += 80;
        smallFont.setColor(DIM_TEXT);
        smallFont.draw(batch, "[R]Repair  [A]Alert  [N]NextEra", cx, y + 2);

        // Beam time remaining
        double remaining = beamTimeManager.getRemaining();
        double total = beamTimeManager.getTotalBudget();
        double pct = total > 0 ? remaining / total * 100 : 0;
        Color btColor = pct > 30 ? ACCENT_GREEN : (pct > 10 ? ACCENT_YELLOW : ACCENT_RED);
        bodyFont.setColor(btColor);
        String btText = String.format("Beam time: %.0fh/%.0fh", remaining, total);
        layout.setText(bodyFont, btText);
        bodyFont.draw(batch, btText, w - layout.width - MARGIN, y);

        // Discovery claim hint on discovery tab
        if (activeTab == Tab.DISCOVERY && discovery.hasClaimableDiscovery()) {
            bodyFont.setColor(ACCENT_GREEN);
            bodyFont.draw(batch, "[D] Claim Discovery!", w * 0.6f, y);
        }
    }

    // ── Tab: Control Room ───────────────────────────────────────────

    private void renderControlRoom(float w, float contentY, float contentH, float delta) {
        float leftW = w * 0.30f;
        float centerW = w * 0.38f;
        float rightW = w - leftW - centerW;

        // Panel backgrounds
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL_BG);
        shapes.rect(MARGIN, contentY, leftW - MARGIN * 2, contentH);
        shapes.rect(leftW, contentY, centerW - MARGIN, contentH);
        shapes.rect(leftW + centerW, contentY, rightW - MARGIN, contentH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(PANEL_BORDER);
        shapes.rect(MARGIN, contentY, leftW - MARGIN * 2, contentH);
        shapes.rect(leftW, contentY, centerW - MARGIN, contentH);
        shapes.rect(leftW + centerW, contentY, rightW - MARGIN, contentH);
        shapes.end();

        batch.begin();
        drawBeamStatus(MARGIN + 8, contentY + contentH - 8);
        drawSubsystems(MARGIN + 8, contentY + contentH * 0.45f);
        drawLuminosityAndMissions(leftW + 8, contentY + contentH - 8);
        drawEventLog(leftW + 8, contentY + contentH * 0.40f);
        drawAlerts(leftW + centerW + 8, contentY + contentH - 8);
        drawProcessCounts(leftW + centerW + 8, contentY + contentH * 0.50f);
        batch.end();
    }

    private void drawBeamStatus(float x, float y) {
        bodyFont.setColor(ACCENT_CYAN);
        bodyFont.draw(batch, "BEAM STATUS", x, y);
        y -= 26;

        Color bc = sim.isBeamOn() ? ACCENT_GREEN : ACCENT_RED;
        bodyFont.setColor(bc);
        bodyFont.draw(batch, sim.isBeamOn() ? ">> BEAM ON <<" : "-- BEAM OFF --", x, y);
        y -= 24;

        if (sim.getBeam1() != null) {
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, String.format("Energy: %.0f GeV/beam",
                    sim.getBeam1().getEnergy()), x, y);
            y -= 16;
            smallFont.draw(batch, String.format("Bunches: %d  N/b: %.2e",
                    SimulationManager.NUM_BUNCHES,
                    sim.getBeam1().getNumParticles()), x, y);
            y -= 16;
            smallFont.draw(batch, String.format("gamma=%.0f  Brho=%.1f T*m",
                    sim.getBeam1().lorentzGamma(),
                    sim.getBeam1().magneticRigidity()), x, y);
        }
    }

    private void drawSubsystems(float x, float y) {
        bodyFont.setColor(ACCENT_CYAN);
        bodyFont.draw(batch, "SUBSYSTEMS", x, y);
        y -= 22;

        for (SubsystemStatus ss : sim.getSubsystems()) {
            Color c = switch (ss.getState()) {
                case NOMINAL  -> ACCENT_GREEN;
                case WARNING  -> ACCENT_YELLOW;
                case CRITICAL -> ACCENT_RED;
                case OFFLINE  -> new Color(0.4f, 0.4f, 0.4f, 1f);
            };
            smallFont.setColor(c);
            smallFont.draw(batch, String.format("%-14s %3.0f%% %s",
                    abbreviate(ss.getName(), 14), ss.getHealthPercent(),
                    ss.getState() == SubsystemStatus.State.NOMINAL ? "OK" : ss.getState().name()), x, y);
            y -= 15;
        }
    }

    private void drawLuminosityAndMissions(float x, float y) {
        bodyFont.setColor(ACCENT_CYAN);
        bodyFont.draw(batch, "LUMINOSITY & DATA", x, y);
        y -= 26;

        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(batch, "Inst. L: " + formatScientific(sim.getInstLuminosity(), "cm^-2s^-1"), x, y);
        y -= 22;
        bodyFont.draw(batch, String.format("Int. L: %.4f fb^-1", sim.getIntegratedLumiFb()), x, y);
        y -= 22;

        // Per-detector luminosity from LumiBank
        var lumiBank = sim.getLumiBank();
        bodyFont.setColor(DIM_TEXT);
        double cmsFb = lumiBank.get("CMS") != null ? lumiBank.get("CMS").totalFb() : 0;
        bodyFont.draw(batch, String.format("  CMS: %.4f fb^-1", cmsFb), x, y);
        y -= 18;

        bodyFont.setColor(ACCENT_GREEN);
        bodyFont.draw(batch, String.format("Total events: %,d", sim.getTotalEvents()), x, y);
        y -= 30;

        bodyFont.setColor(ACCENT_YELLOW);
        bodyFont.draw(batch, "MISSIONS", x, y);
        y -= 22;

        Era era = sim.getCurrentEra();
        List<String> claimed = discovery.getClaimedDiscoveries();
        for (String mission : era.missions()) {
            boolean done = claimed.stream().anyMatch(d -> d.toLowerCase().contains(
                    mission.replace("discover_", "").replace("_", " ")));
            smallFont.setColor(done ? ACCENT_GREEN : Color.WHITE);
            String prefix = done ? "v " : "> ";
            smallFont.draw(batch, prefix + mission.replace("_", " "), x, y);
            y -= 16;
        }
    }

    private void drawEventLog(float x, float y) {
        bodyFont.setColor(ACCENT_CYAN);
        bodyFont.draw(batch, "EVENT LOG", x, y);
        y -= 22;

        smallFont.setColor(new Color(0.7f, 0.8f, 0.9f, 1f));
        for (int i = 0; i < EVENT_LOG_SIZE; i++) {
            int idx = (eventLogHead - 1 - i + EVENT_LOG_SIZE) % EVENT_LOG_SIZE;
            if (eventLog[idx] != null) {
                smallFont.draw(batch, eventLog[idx], x, y);
                y -= 14;
            }
        }
    }

    private void drawAlerts(float x, float y) {
        bodyFont.setColor(ACCENT_RED);
        bodyFont.draw(batch, "ALERTS", x, y);
        y -= 24;

        List<AlertSystem.Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            smallFont.setColor(ACCENT_GREEN);
            smallFont.draw(batch, "No active alerts", x, y);
        } else {
            for (AlertSystem.Alert alert : alerts) {
                Color ac = switch (alert.severity()) {
                    case INFO     -> ACCENT_CYAN;
                    case WARNING  -> ACCENT_YELLOW;
                    case CRITICAL -> ACCENT_RED;
                };
                smallFont.setColor(ac);
                smallFont.draw(batch, String.format("[%c] %s (%.0fs)",
                        alert.severity().name().charAt(0),
                        abbreviate(alert.message(), 32),
                        alert.timeRemainingSeconds()), x, y);
                y -= 16;
            }
        }
    }

    private void drawProcessCounts(float x, float y) {
        bodyFont.setColor(ACCENT_CYAN);
        bodyFont.draw(batch, "PROCESS COUNTS", x, y);
        y -= 22;

        Map<String, Long> counts = sim.getEventsByProcess();
        if (counts.isEmpty()) {
            smallFont.setColor(DIM_TEXT);
            smallFont.draw(batch, "No events yet", x, y);
        } else {
            for (var entry : counts.entrySet()) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, String.format("%-18s %,8d",
                        entry.getKey(), entry.getValue()), x, y);
                y -= 15;
            }
        }
    }

    // ── Tab: Ring View ──────────────────────────────────────────────

    private void renderRingView(float w, float contentY, float contentH, float delta) {
        Era era = sim.getCurrentEra();
        double energy = sim.getBeam1() != null ? sim.getBeam1().getEnergy() : 0;

        // The ring renderer manages its own shape/batch begin/end calls
        ringView.render(batch, shapes, bodyFont,
                w, contentH, delta,
                sim.isBeamOn(), energy, era.activeMachine());
    }

    // ── Tab: Event Display ──────────────────────────────────────────

    private void renderEventDisplay(float w, float contentY, float contentH, float delta) {
        float radius = Math.min(w, contentH) * 0.42f;
        float cx = w * 0.5f;
        float cy = contentY + contentH * 0.5f;

        eventDisplay.render(batch, shapes, bodyFont,
                cx, cy, radius,
                lastRecoObjects, animTime);

        // Draw a small info overlay
        batch.begin();
        smallFont.setColor(DIM_TEXT);
        String info = lastRecoObjects.isEmpty()
                ? "Waiting for events... (press SPACE to start beam)"
                : String.format("%d reconstructed objects", lastRecoObjects.size());
        layout.setText(smallFont, info);
        smallFont.draw(batch, info, w * 0.5f - layout.width * 0.5f, contentY + 16);
        batch.end();
    }

    // ── Tab: Discovery ──────────────────────────────────────────────

    private void renderDiscovery(float w, float contentY, float contentH, float delta) {
        Era era = sim.getCurrentEra();
        discovery.render(batch, shapes, bodyFont, smallFont,
                MARGIN, contentY, w - MARGIN * 2, contentH, animTime,
                sim.getIntegratedLumiFb(), era.name());

        // Show claimed discoveries
        if (!discovery.getClaimedDiscoveries().isEmpty()) {
            batch.begin();
            bodyFont.setColor(ACCENT_GREEN);
            StringBuilder sb = new StringBuilder("Discoveries: ");
            for (String d : discovery.getClaimedDiscoveries()) {
                sb.append(d).append("  ");
            }
            bodyFont.draw(batch, sb.toString(), MARGIN + 10, contentY + 18);
            batch.end();
        }
    }

    // ── Utility ─────────────────────────────────────────────────────

    private static String abbreviate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 2) + "..";
    }

    private static String formatScientific(double val, String unit) {
        if (val == 0) return "0 " + unit;
        int exp = (int) Math.floor(Math.log10(Math.abs(val)));
        double mantissa = val / Math.pow(10, exp);
        return String.format("%.2fx10^%d %s", mantissa, exp, unit);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (headerFont != null) headerFont.dispose();
        if (bodyFont != null) bodyFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
