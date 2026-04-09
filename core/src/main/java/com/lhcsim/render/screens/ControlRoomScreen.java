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
import com.lhcsim.render.LHCSimGame;

import java.util.List;
import java.util.Map;

/**
 * Main gameplay screen: the LHC Control Room.
 * <p>
 * Displays beam status, subsystem health, luminosity/event counters,
 * alerts, event log, and time/beam controls. The player manages the
 * accelerator through campaign eras, collecting data and responding
 * to alerts.
 */
public class ControlRoomScreen extends ScreenAdapter {

    private static final Color BG_COLOR = new Color(0x08 / 255f, 0x0C / 255f, 0x1A / 255f, 1f);
    private static final Color PANEL_BG = new Color(0x10 / 255f, 0x18 / 255f, 0x30 / 255f, 1f);
    private static final Color PANEL_BORDER = new Color(0x20 / 255f, 0x40 / 255f, 0x70 / 255f, 1f);
    private static final Color ACCENT_BLUE = new Color(0.3f, 0.6f, 1.0f, 1f);
    private static final Color ACCENT_GREEN = new Color(0.2f, 0.9f, 0.3f, 1f);
    private static final Color ACCENT_YELLOW = new Color(1.0f, 0.85f, 0.2f, 1f);
    private static final Color ACCENT_RED = new Color(1.0f, 0.25f, 0.25f, 1f);
    private static final Color DIM_TEXT = new Color(0.5f, 0.55f, 0.65f, 1f);

    private final LHCSimGame game;
    private final SimulationManager sim;
    private final TimeManager timeManager;
    private final BeamTimeManager beamTimeManager;
    private final AlertSystem alertSystem;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont headerFont;
    private BitmapFont bodyFont;
    private BitmapFont smallFont;
    private GlyphLayout layout;

    // Event log ring buffer
    private static final int EVENT_LOG_SIZE = 12;
    private final String[] eventLog = new String[EVENT_LOG_SIZE];
    private int eventLogHead = 0;

    public ControlRoomScreen(LHCSimGame game, SimulationManager sim) {
        this.game = game;
        this.sim = sim;
        this.timeManager = game.getTimeManager();
        this.beamTimeManager = game.getBeamTimeManager();
        this.alertSystem = game.getAlertSystem();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        layout = new GlyphLayout();

        headerFont = new BitmapFont();
        headerFont.setColor(Color.WHITE);
        headerFont.getData().setScale(2.0f);

        bodyFont = new BitmapFont();
        bodyFont.setColor(Color.WHITE);
        bodyFont.getData().setScale(1.3f);

        smallFont = new BitmapFont();
        smallFont.setColor(DIM_TEXT);
        smallFont.getData().setScale(1.0f);

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

    private boolean handleKeyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.SPACE -> sim.toggleBeam();
            case Input.Keys.NUM_1 -> timeManager.setMode(TimeManager.TimeMode.PAUSED);
            case Input.Keys.NUM_2 -> timeManager.setMode(TimeManager.TimeMode.NORMAL);
            case Input.Keys.NUM_3 -> timeManager.setMode(TimeManager.TimeMode.FAST);
            case Input.Keys.N -> sim.advanceEra();
            case Input.Keys.R -> {
                // Repair most damaged subsystem
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
            case Input.Keys.A -> {
                // Acknowledge oldest alert
                List<AlertSystem.Alert> alerts = alertSystem.getActiveAlerts();
                if (!alerts.isEmpty()) {
                    alertSystem.acknowledgeAlert(alerts.get(0).id());
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private boolean handleClick(int x, int y) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Beam toggle button area (bottom-left)
        if (x < w * 0.2f && y < 60) {
            sim.toggleBeam();
            return true;
        }
        // Time control buttons (bottom-center)
        if (x > w * 0.3f && x < w * 0.7f && y < 60) {
            float section = (x - w * 0.3f) / (w * 0.4f);
            if (section < 0.33f) {
                timeManager.setMode(TimeManager.TimeMode.PAUSED);
            } else if (section < 0.66f) {
                timeManager.setMode(TimeManager.TimeMode.NORMAL);
            } else {
                timeManager.setMode(TimeManager.TimeMode.FAST);
            }
            return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        // Update simulation
        if (timeManager.getMode() != TimeManager.TimeMode.PAUSED) {
            sim.update(delta);
        }

        // Log new events
        for (PhysicsEvent ev : sim.getLastBatchEvents()) {
            eventLog[eventLogHead] = String.format("Event #%d  %s  sqrt(s)=%.0f GeV",
                    ev.eventNumber(), ev.processName(), ev.sqrtS());
            eventLogHead = (eventLogHead + 1) % EVENT_LOG_SIZE;
        }

        // Clear
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, BG_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float margin = 12;

        // Draw panels with ShapeRenderer
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawPanels(w, h, margin);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        drawPanelBorders(w, h, margin);
        shapes.end();

        // Draw text
        batch.begin();
        drawHeader(w, h, margin);
        drawBeamStatus(w, h, margin);
        drawSubsystems(w, h, margin);
        drawLuminosity(w, h, margin);
        drawEventLog(w, h, margin);
        drawAlerts(w, h, margin);
        drawProcessCounts(w, h, margin);
        drawControls(w, h, margin);
        batch.end();
    }

    // ── Panel layout ────────────────────────────────────────────────
    // Layout: Top bar | Left col (beam + subsys) | Center (lumi + events) | Right (alerts + processes)
    // Bottom: controls bar

    private void drawPanels(float w, float h, float m) {
        float topH = 50;
        float botH = 55;
        float midH = h - topH - botH - m * 3;

        // Top bar
        shapes.setColor(PANEL_BG);
        shapes.rect(m, h - topH - m, w - 2 * m, topH);

        // Left column
        float leftW = w * 0.25f;
        shapes.rect(m, botH + m, leftW - m, midH);

        // Center column
        float centerX = leftW + m;
        float centerW = w * 0.42f;
        shapes.rect(centerX, botH + m, centerW - m, midH);

        // Right column
        float rightX = centerX + centerW;
        float rightW = w - rightX - m;
        shapes.rect(rightX, botH + m, rightW, midH);

        // Bottom bar
        shapes.rect(m, m, w - 2 * m, botH - m);
    }

    private void drawPanelBorders(float w, float h, float m) {
        float topH = 50;
        float botH = 55;
        float midH = h - topH - botH - m * 3;

        shapes.setColor(PANEL_BORDER);
        shapes.rect(m, h - topH - m, w - 2 * m, topH);
        float leftW = w * 0.25f;
        shapes.rect(m, botH + m, leftW - m, midH);
        float centerX = leftW + m;
        float centerW = w * 0.42f;
        shapes.rect(centerX, botH + m, centerW - m, midH);
        float rightX = centerX + centerW;
        float rightW = w - rightX - m;
        shapes.rect(rightX, botH + m, rightW, midH);
        shapes.rect(m, m, w - 2 * m, botH - m);
    }

    // ── Drawing helpers ─────────────────────────────────────────────

    private void drawHeader(float w, float h, float m) {
        Era era = sim.getCurrentEra();
        float y = h - m - 8;

        headerFont.setColor(ACCENT_BLUE);
        headerFont.draw(batch, "LHC CONTROL ROOM", m + 10, y);

        bodyFont.setColor(Color.WHITE);
        String eraText = String.format("Era %d: %s  |  %s  |  sqrt(s) = %.1f TeV",
                era.number(), era.name(), era.activeMachine(), era.sqrtS());
        layout.setText(bodyFont, eraText);
        bodyFont.draw(batch, eraText, w - layout.width - m - 10, y - 5);

        // Year/Day
        smallFont.setColor(DIM_TEXT);
        String timeText = String.format("Year %d  Day %d  [%s]",
                timeManager.getYear(), timeManager.getDay(),
                timeManager.getMode().name());
        layout.setText(smallFont, timeText);
        smallFont.draw(batch, timeText, w / 2 - layout.width / 2, y - 8);
    }

    private void drawBeamStatus(float w, float h, float m) {
        float leftW = w * 0.25f;
        float topH = 50;
        float botH = 55;
        float panelTop = h - topH - m * 2;
        float x = m + 10;
        float y = panelTop - 5;

        // Section title
        bodyFont.setColor(ACCENT_BLUE);
        bodyFont.draw(batch, "BEAM STATUS", x, y);
        y -= 30;

        // Beam on/off indicator
        Color beamColor = sim.isBeamOn() ? ACCENT_GREEN : ACCENT_RED;
        bodyFont.setColor(beamColor);
        bodyFont.draw(batch, sim.isBeamOn() ? ">> BEAM ON <<" : "-- BEAM OFF --", x, y);
        y -= 28;

        if (sim.getBeam1() != null) {
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, String.format("Energy: %.0f GeV/beam",
                    sim.getBeam1().getEnergy()), x, y);
            y -= 18;
            smallFont.draw(batch, String.format("Bunches: %d", 2808), x, y);
            y -= 18;
            smallFont.draw(batch, String.format("N/bunch: %.2e",
                    sim.getBeam1().getNumParticles()), x, y);
            y -= 18;
            smallFont.draw(batch, String.format("gamma: %.0f",
                    sim.getBeam1().lorentzGamma()), x, y);
            y -= 18;
            smallFont.draw(batch, String.format("B-rho: %.1f T*m",
                    sim.getBeam1().magneticRigidity()), x, y);
            y -= 18;
        }

        // Beam time
        y -= 10;
        bodyFont.setColor(ACCENT_YELLOW);
        bodyFont.draw(batch, "BEAM TIME", x, y);
        y -= 24;

        double remaining = beamTimeManager.getRemaining();
        double total = beamTimeManager.getTotalBudget();
        double pct = total > 0 ? remaining / total * 100 : 0;
        Color btColor = pct > 30 ? ACCENT_GREEN : (pct > 10 ? ACCENT_YELLOW : ACCENT_RED);
        smallFont.setColor(btColor);
        smallFont.draw(batch, String.format("%.0f / %.0f hrs (%.0f%%)",
                remaining, total, pct), x, y);
    }

    private void drawSubsystems(float w, float h, float m) {
        float leftW = w * 0.25f;
        float botH = 55;
        float x = m + 10;
        float y = botH + m + 220;

        bodyFont.setColor(ACCENT_BLUE);
        bodyFont.draw(batch, "SUBSYSTEMS", x, y);
        y -= 26;

        for (SubsystemStatus ss : sim.getSubsystems()) {
            Color stateColor = switch (ss.getState()) {
                case NOMINAL -> ACCENT_GREEN;
                case WARNING -> ACCENT_YELLOW;
                case CRITICAL -> ACCENT_RED;
                case OFFLINE -> new Color(0.5f, 0.5f, 0.5f, 1f);
            };
            smallFont.setColor(stateColor);

            String line = String.format("%-12s %3.0f%% %s",
                    abbreviate(ss.getName(), 12), ss.getHealthPercent(),
                    ss.getState() == SubsystemStatus.State.NOMINAL ? "OK" : ss.getState().name());
            smallFont.draw(batch, line, x, y);
            y -= 17;
        }

        y -= 8;
        smallFont.setColor(DIM_TEXT);
        smallFont.draw(batch, "[R] Repair worst system", x, y);
    }

    private void drawLuminosity(float w, float h, float m) {
        float leftW = w * 0.25f;
        float topH = 50;
        float centerX = leftW + m + 10;
        float panelTop = h - topH - m * 2;
        float y = panelTop - 5;

        bodyFont.setColor(ACCENT_BLUE);
        bodyFont.draw(batch, "LUMINOSITY & DATA", centerX, y);
        y -= 30;

        bodyFont.setColor(Color.WHITE);
        String instStr = formatScientific(sim.getInstLuminosity(), "cm^-2 s^-1");
        bodyFont.draw(batch, "Inst. L: " + instStr, centerX, y);
        y -= 28;

        bodyFont.draw(batch, String.format("Int. L: %.4f fb^-1", sim.getIntegratedLumiFb()), centerX, y);
        y -= 28;

        bodyFont.setColor(ACCENT_GREEN);
        bodyFont.draw(batch, String.format("Total events: %,d", sim.getTotalEvents()), centerX, y);
        y -= 35;

        // Missions
        bodyFont.setColor(ACCENT_YELLOW);
        bodyFont.draw(batch, "MISSIONS", centerX, y);
        y -= 24;

        Era era = sim.getCurrentEra();
        for (String mission : era.missions()) {
            smallFont.setColor(Color.WHITE);
            String displayName = mission.replace("_", " ");
            smallFont.draw(batch, "  > " + displayName, centerX, y);
            y -= 18;
        }

        y -= 10;
        smallFont.setColor(DIM_TEXT);
        smallFont.draw(batch, "[N] Advance to next era", centerX, y);
    }

    private void drawEventLog(float w, float h, float m) {
        float leftW = w * 0.25f;
        float centerX = leftW + m + 10;
        float botH = 55;
        float y = botH + m + 210;

        bodyFont.setColor(ACCENT_BLUE);
        bodyFont.draw(batch, "EVENT LOG", centerX, y);
        y -= 24;

        smallFont.setColor(new Color(0.7f, 0.8f, 0.9f, 1f));
        // Show recent events from ring buffer (newest first)
        for (int i = 0; i < EVENT_LOG_SIZE; i++) {
            int idx = (eventLogHead - 1 - i + EVENT_LOG_SIZE) % EVENT_LOG_SIZE;
            if (eventLog[idx] != null) {
                smallFont.draw(batch, eventLog[idx], centerX, y);
                y -= 16;
            }
        }
    }

    private void drawAlerts(float w, float h, float m) {
        float leftW = w * 0.25f;
        float centerW = w * 0.42f;
        float rightX = leftW + centerW + m + 10;
        float topH = 50;
        float panelTop = h - topH - m * 2;
        float y = panelTop - 5;

        bodyFont.setColor(ACCENT_RED);
        bodyFont.draw(batch, "ALERTS", rightX, y);
        y -= 26;

        List<AlertSystem.Alert> alerts = alertSystem.getActiveAlerts();
        if (alerts.isEmpty()) {
            smallFont.setColor(ACCENT_GREEN);
            smallFont.draw(batch, "No active alerts", rightX, y);
            y -= 18;
        } else {
            for (AlertSystem.Alert alert : alerts) {
                Color aColor = switch (alert.severity()) {
                    case INFO -> ACCENT_BLUE;
                    case WARNING -> ACCENT_YELLOW;
                    case CRITICAL -> ACCENT_RED;
                };
                smallFont.setColor(aColor);
                String aLine = String.format("[%s] %s (%.0fs)",
                        alert.severity().name().charAt(0),
                        abbreviate(alert.message(), 35),
                        alert.timeRemainingSeconds());
                smallFont.draw(batch, aLine, rightX, y);
                y -= 17;
            }
        }

        y -= 8;
        smallFont.setColor(DIM_TEXT);
        smallFont.draw(batch, "[A] Acknowledge alert", rightX, y);
    }

    private void drawProcessCounts(float w, float h, float m) {
        float leftW = w * 0.25f;
        float centerW = w * 0.42f;
        float rightX = leftW + centerW + m + 10;
        float botH = 55;
        float y = botH + m + 260;

        bodyFont.setColor(ACCENT_BLUE);
        bodyFont.draw(batch, "PROCESS COUNTS", rightX, y);
        y -= 24;

        Map<String, Long> counts = sim.getEventsByProcess();
        if (counts.isEmpty()) {
            smallFont.setColor(DIM_TEXT);
            smallFont.draw(batch, "No events yet", rightX, y);
        } else {
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, String.format("%-20s %,8d",
                        entry.getKey(), entry.getValue()), rightX, y);
                y -= 17;
            }
        }
    }

    private void drawControls(float w, float h, float m) {
        float y = m + 35;

        // Beam toggle
        Color beamBtnColor = sim.isBeamOn() ? ACCENT_GREEN : ACCENT_RED;
        bodyFont.setColor(beamBtnColor);
        bodyFont.draw(batch, sim.isBeamOn() ? "[SPACE] Stop Beam" : "[SPACE] Start Beam",
                m + 10, y);

        // Time controls
        float centerX = w * 0.3f + 10;
        TimeManager.TimeMode mode = timeManager.getMode();

        bodyFont.setColor(mode == TimeManager.TimeMode.PAUSED ? ACCENT_YELLOW : DIM_TEXT);
        bodyFont.draw(batch, "[1] Pause", centerX, y);

        centerX += 130;
        bodyFont.setColor(mode == TimeManager.TimeMode.NORMAL ? ACCENT_GREEN : DIM_TEXT);
        bodyFont.draw(batch, "[2] Normal", centerX, y);

        centerX += 140;
        bodyFont.setColor(mode == TimeManager.TimeMode.FAST ? ACCENT_BLUE : DIM_TEXT);
        bodyFont.draw(batch, "[3] Fast", centerX, y);

        // Era info
        float rightX = w - 280;
        smallFont.setColor(DIM_TEXT);
        smallFont.draw(batch, String.format("Era %d/%d  |  [N] Next Era",
                sim.getEraIndex() + 1, sim.getAllEras().size()), rightX, y - 5);
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
        return String.format("%.2f x 10^%d %s", mantissa, exp, unit);
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
