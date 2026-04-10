package com.lhcsim.render.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.lhcsim.physics.accelerator.Lattice;
import com.lhcsim.physics.accelerator.LatticeLoader;
import com.lhcsim.physics.beam.Bunch;
import com.lhcsim.render.ring.BetaFunctionPlot;

/**
 * Draws a top-down schematic of the LHC accelerator ring as an octagon
 * with 8 interaction points, animated beam lines, and magnet indicators.
 * Styled to resemble a CERN control-room display.
 */
public class RingViewRenderer {

    private static final Color BG_COLOR       = new Color(0x0A0E1FFF);
    private static final Color BEAM1_COLOR    = new Color(0x00D4FFFF); // cyan – clockwise
    private static final Color BEAM2_COLOR    = new Color(0xFF6B35FF); // orange – counter-clockwise
    private static final Color DIPOLE_COLOR   = new Color(0x336699FF); // CERN cryostat blue
    private static final Color QUAD_F_COLOR   = new Color(0xCC3333FF); // focusing quad
    private static final Color QUAD_D_COLOR   = new Color(0x33AA55FF); // defocusing quad
    private static final Color RING_COLOR     = new Color(0x1A2744FF);
    private static final Color GRID_COLOR     = new Color(0x0F1A33FF);
    private static final Color LABEL_DIM      = new Color(0x4477AAFF);
    private static final Color IP_GLOW_COLOR  = new Color(0x00D4FF44);

    private static final String[] IP_NAMES = {
        "IP1 ATLAS", "IP2 ALICE", "IP3", "IP4",
        "IP5 CMS",   "IP6",       "IP7", "IP8 LHCb"
    };

    /** Pre-computed angles for 8 IPs: IP1 at top (90°), proceeding clockwise. */
    private static final float[] IP_ANGLES = new float[8];
    static {
        for (int i = 0; i < 8; i++) {
            // IP1 = 90°, each subsequent IP is 45° clockwise (subtract)
            IP_ANGLES[i] = 90f - i * 45f;
        }
    }

    private static final int BUNCHES_PER_BEAM = 18;
    private static final int DIPOLES_PER_ARC  = 6;
    private static final int QUADS_PER_ARC    = 3;

    private float phaseBeam1;
    private float phaseBeam2;
    private float glowTimer;

    private final GlyphLayout layout = new GlyphLayout();

    /** Beta function plot rendered below the ring schematic. */
    private final BetaFunctionPlot betaPlot = new BetaFunctionPlot();
    private boolean betaPlotInitialised;

    /**
     * Lazily initialises the beta function plot from the PS lattice.
     * Called once on first render.
     */
    private void ensureBetaPlot() {
        if (betaPlotInitialised) return;
        betaPlotInitialised = true;
        try {
            Lattice ps = LatticeLoader.loadResource("/data/lattices/ps.json");
            double gamma = 26.0 / Bunch.PROTON_MASS;
            betaPlot.update(ps, gamma);
        } catch (Exception e) {
            // If the PS lattice can't be loaded, silently skip the plot
        }
    }

    // ── public API ─────────────────────────────────────────────────────

    /**
     * Render the ring schematic into the given area.
     *
     * @param batch       active SpriteBatch (caller manages begin/end)
     * @param shapes      ShapeRenderer (this method will call begin/end)
     * @param font        BitmapFont for labels
     * @param w           available width  (pixels)
     * @param h           available height (pixels)
     * @param delta       frame delta-time (seconds)
     * @param beamOn      whether beams are circulating
     * @param energy      beam energy in GeV (used for display only)
     * @param machineName label drawn at ring centre ("LHC", "HL-LHC", …)
     */
    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                       float w, float h, float delta,
                       boolean beamOn, double energy, String machineName) {

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float radius = Math.min(w, h) * 0.38f;

        if (beamOn) {
            phaseBeam1 += delta * 55f;  // degrees per second – clockwise
            phaseBeam2 -= delta * 55f;  // counter-clockwise
            glowTimer  += delta;
        }

        // ── shapes pass (filled) ───────────────────────────────────────
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawGrid(shapes, w, h);
        drawRingFill(shapes, cx, cy, radius);
        drawSectorTints(shapes, cx, cy, radius);
        drawMagnets(shapes, cx, cy, radius);

        if (beamOn) {
            drawIPGlows(shapes, cx, cy, radius);
        }

        shapes.end();

        // ── shapes pass (lines) ────────────────────────────────────────
        shapes.begin(ShapeRenderer.ShapeType.Line);

        drawRingOutline(shapes, cx, cy, radius);
        drawTickMarks(shapes, cx, cy, radius);

        if (beamOn) {
            drawBeamLine(shapes, cx, cy, radius, BEAM1_COLOR, 1.5f);
            drawBeamLine(shapes, cx, cy, radius, BEAM2_COLOR, -1.5f);
        }

        shapes.end();

        // ── shapes pass (filled – dots on top) ─────────────────────────
        if (beamOn) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            drawBunchDots(shapes, cx, cy, radius, phaseBeam1, BEAM1_COLOR, 1.5f);
            drawBunchDots(shapes, cx, cy, radius, phaseBeam2, BEAM2_COLOR, -1.5f);
            shapes.end();
        }

        // ── text pass ──────────────────────────────────────────────────
        drawIPMarkers(shapes, cx, cy, radius);

        batch.begin();
        drawIPLabels(batch, font, cx, cy, radius);
        drawCentreLabel(batch, font, cx, cy, machineName, energy);
        batch.end();

        // ── beta function plot (bottom strip) ──────────────────────
        ensureBetaPlot();
        float plotH = h * 0.18f;
        float plotY = 4f;
        float plotX = 40f;
        float plotW = w - 80f;
        betaPlot.render(shapes, batch, font, plotX, plotY, plotW, plotH);
    }

    // ── background grid ────────────────────────────────────────────────

    private void drawGrid(ShapeRenderer s, float w, float h) {
        s.setColor(GRID_COLOR);
        float step = 40f;
        for (float x = 0; x < w; x += step) {
            s.rectLine(x, 0, x, h, 1f);
        }
        for (float y = 0; y < h; y += step) {
            s.rectLine(0, y, w, y, 1f);
        }
    }

    // ── octagonal ring ─────────────────────────────────────────────────

    private void drawRingFill(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(RING_COLOR);
        float inner = r - 6f;
        float outer = r + 6f;
        for (int i = 0; i < 8; i++) {
            float a1 = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float a2 = IP_ANGLES[(i + 1) % 8] * MathUtils.degreesToRadians;
            float x1i = cx + inner * MathUtils.cos(a1);
            float y1i = cy + inner * MathUtils.sin(a1);
            float x1o = cx + outer * MathUtils.cos(a1);
            float y1o = cy + outer * MathUtils.sin(a1);
            float x2i = cx + inner * MathUtils.cos(a2);
            float y2i = cy + inner * MathUtils.sin(a2);
            float x2o = cx + outer * MathUtils.cos(a2);
            float y2o = cy + outer * MathUtils.sin(a2);
            s.triangle(x1i, y1i, x1o, y1o, x2o, y2o);
            s.triangle(x1i, y1i, x2o, y2o, x2i, y2i);
        }
    }

    private void drawRingOutline(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(LABEL_DIM);
        for (int i = 0; i < 8; i++) {
            float a1 = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float a2 = IP_ANGLES[(i + 1) % 8] * MathUtils.degreesToRadians;
            s.line(cx + r * MathUtils.cos(a1), cy + r * MathUtils.sin(a1),
                   cx + r * MathUtils.cos(a2), cy + r * MathUtils.sin(a2));
        }
    }

    // ── subtle sector tints between IPs ────────────────────────────────

    private void drawSectorTints(ShapeRenderer s, float cx, float cy, float r) {
        float tintAlpha = 0.06f;
        for (int i = 0; i < 8; i++) {
            float hue = i / 8f;
            Color c = hsvToColor(hue, 0.5f, 0.6f, tintAlpha);
            s.setColor(c);
            float a1 = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float a2 = IP_ANGLES[(i + 1) % 8] * MathUtils.degreesToRadians;
            float ri = r - 5f;
            float ro = r + 5f;
            float x1i = cx + ri * MathUtils.cos(a1), y1i = cy + ri * MathUtils.sin(a1);
            float x1o = cx + ro * MathUtils.cos(a1), y1o = cy + ro * MathUtils.sin(a1);
            float x2i = cx + ri * MathUtils.cos(a2), y2i = cy + ri * MathUtils.sin(a2);
            float x2o = cx + ro * MathUtils.cos(a2), y2o = cy + ro * MathUtils.sin(a2);
            s.triangle(x1i, y1i, x1o, y1o, x2o, y2o);
            s.triangle(x1i, y1i, x2o, y2o, x2i, y2i);
        }
    }

    // ── magnets along arcs ─────────────────────────────────────────────

    private void drawMagnets(ShapeRenderer s, float cx, float cy, float r) {
        for (int arc = 0; arc < 8; arc++) {
            float startDeg = IP_ANGLES[arc];
            float endDeg   = IP_ANGLES[(arc + 1) % 8];
            if (endDeg > startDeg) endDeg -= 360f;
            float span = endDeg - startDeg;

            // dipoles
            for (int d = 1; d <= DIPOLES_PER_ARC; d++) {
                float t = d / (float) (DIPOLES_PER_ARC + QUADS_PER_ARC + 1);
                float deg = startDeg + span * t;
                float rad = deg * MathUtils.degreesToRadians;
                drawMagnetRect(s, cx, cy, r, rad, 4f, 2f, DIPOLE_COLOR);
            }
            // quadrupoles (F/D alternating)
            for (int q = 1; q <= QUADS_PER_ARC; q++) {
                float t = (DIPOLES_PER_ARC + q) / (float) (DIPOLES_PER_ARC + QUADS_PER_ARC + 1);
                float deg = startDeg + span * t;
                float rad = deg * MathUtils.degreesToRadians;
                Color qc = (q % 2 == 0) ? QUAD_D_COLOR : QUAD_F_COLOR;
                drawMagnetRect(s, cx, cy, r, rad, 3f, 3f, qc);
            }
        }
    }

    private void drawMagnetRect(ShapeRenderer s, float cx, float cy, float r,
                                float angleRad, float halfW, float halfH, Color c) {
        s.setColor(c);
        float mx = cx + r * MathUtils.cos(angleRad);
        float my = cy + r * MathUtils.sin(angleRad);
        // tangent direction for rotation
        float tx = -MathUtils.sin(angleRad);
        float ty =  MathUtils.cos(angleRad);
        float nx =  MathUtils.cos(angleRad);
        float ny =  MathUtils.sin(angleRad);
        // four corners of the rotated rectangle
        float ax1 = mx + tx * halfW + nx * halfH;
        float ay1 = my + ty * halfW + ny * halfH;
        float bx1 = mx - tx * halfW + nx * halfH;
        float by1 = my - ty * halfW + ny * halfH;
        float cx1 = mx - tx * halfW - nx * halfH;
        float cy1 = my - ty * halfW - ny * halfH;
        float dx1 = mx + tx * halfW - nx * halfH;
        float dy1 = my + ty * halfW - ny * halfH;
        s.triangle(ax1, ay1, bx1, by1, cx1, cy1);
        s.triangle(ax1, ay1, cx1, cy1, dx1, dy1);
    }

    // ── tick marks around the ring ─────────────────────────────────────

    private void drawTickMarks(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(GRID_COLOR);
        int ticks = 64;
        for (int i = 0; i < ticks; i++) {
            float deg = i * (360f / ticks);
            float rad = deg * MathUtils.degreesToRadians;
            float ri = r - 10f;
            float ro = r + 10f;
            s.line(cx + ri * MathUtils.cos(rad), cy + ri * MathUtils.sin(rad),
                   cx + ro * MathUtils.cos(rad), cy + ro * MathUtils.sin(rad));
        }
    }

    // ── beam lines (thin offset from ring) ─────────────────────────────

    private void drawBeamLine(ShapeRenderer s, float cx, float cy, float r,
                              Color color, float offset) {
        s.setColor(color);
        float br = r + offset;
        for (int i = 0; i < 8; i++) {
            float a1 = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float a2 = IP_ANGLES[(i + 1) % 8] * MathUtils.degreesToRadians;
            s.line(cx + br * MathUtils.cos(a1), cy + br * MathUtils.sin(a1),
                   cx + br * MathUtils.cos(a2), cy + br * MathUtils.sin(a2));
        }
    }

    // ── animated bunch dots ────────────────────────────────────────────

    private void drawBunchDots(ShapeRenderer s, float cx, float cy, float r,
                               float phaseDeg, Color color, float offset) {
        s.setColor(color);
        float br = r + offset;
        float dotR = Math.max(2f, r * 0.015f);
        for (int i = 0; i < BUNCHES_PER_BEAM; i++) {
            float deg = phaseDeg + i * (360f / BUNCHES_PER_BEAM);
            // project onto octagon edge
            float rad = deg * MathUtils.degreesToRadians;
            float rawX = MathUtils.cos(rad);
            float rawY = MathUtils.sin(rad);
            float scale = octagonRadius(rawX, rawY);
            s.circle(cx + br * scale * rawX, cy + br * scale * rawY, dotR, 8);
        }
    }

    /**
     * Returns the distance-from-centre scale factor so that (cos θ, sin θ)
     * is projected onto a regular octagon of unit "radius" (vertex distance).
     */
    private float octagonRadius(float ux, float uy) {
        // For a regular octagon with vertices at unit distance from centre,
        // the perpendicular distance to each edge is cos(π/8).
        // The scale for a direction (ux, uy) hitting an edge with normal n is
        // cos(π/8) / dot(dir, n).  We take the minimum over all 8 edges.
        float best = Float.MAX_VALUE;
        float cos22 = MathUtils.cos(MathUtils.PI / 8f);
        for (int i = 0; i < 8; i++) {
            float midAngle = (IP_ANGLES[i] + IP_ANGLES[(i + 1) % 8]) * 0.5f;
            if (IP_ANGLES[(i + 1) % 8] > IP_ANGLES[i]) midAngle -= 180f;
            float mr = midAngle * MathUtils.degreesToRadians;
            float nx = MathUtils.cos(mr);
            float ny = MathUtils.sin(mr);
            float dot = ux * nx + uy * ny;
            if (dot > 1e-6f) {
                best = Math.min(best, cos22 / dot);
            }
        }
        return Math.min(best, 1f);
    }

    // ── IP markers and glows ───────────────────────────────────────────

    private void drawIPGlows(ShapeRenderer s, float cx, float cy, float r) {
        float pulse = 0.3f + 0.7f * (0.5f + 0.5f * MathUtils.sin(glowTimer * 3f));
        for (int i = 0; i < 8; i++) {
            if (!isMainIP(i)) continue;
            float rad = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float ix = cx + r * MathUtils.cos(rad);
            float iy = cy + r * MathUtils.sin(rad);
            Color gc = new Color(IP_GLOW_COLOR.r, IP_GLOW_COLOR.g, IP_GLOW_COLOR.b,
                                 IP_GLOW_COLOR.a * pulse);
            s.setColor(gc);
            s.circle(ix, iy, 14f, 16);
        }
    }

    private void drawIPMarkers(ShapeRenderer s, float cx, float cy, float r) {
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 8; i++) {
            float rad = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float ix = cx + r * MathUtils.cos(rad);
            float iy = cy + r * MathUtils.sin(rad);
            s.setColor(isMainIP(i) ? BEAM1_COLOR : LABEL_DIM);
            s.circle(ix, iy, isMainIP(i) ? 6f : 3f, 12);
        }
        s.end();
    }

    private void drawIPLabels(SpriteBatch batch, BitmapFont font, float cx, float cy, float r) {
        font.setColor(LABEL_DIM);
        float labelR = r + 22f;
        for (int i = 0; i < 8; i++) {
            String name = IP_NAMES[i];
            float rad = IP_ANGLES[i] * MathUtils.degreesToRadians;
            float lx = cx + labelR * MathUtils.cos(rad);
            float ly = cy + labelR * MathUtils.sin(rad);
            layout.setText(font, name);
            if (isMainIP(i)) font.setColor(BEAM1_COLOR);
            font.draw(batch, name, lx - layout.width * 0.5f, ly + layout.height * 0.5f);
            if (isMainIP(i)) font.setColor(LABEL_DIM);
        }
    }

    // ── centre label ───────────────────────────────────────────────────

    private void drawCentreLabel(SpriteBatch batch, BitmapFont font,
                                 float cx, float cy, String name, double energy) {
        font.setColor(BEAM1_COLOR);
        layout.setText(font, name);
        font.draw(batch, name, cx - layout.width * 0.5f, cy + layout.height + 2f);

        String eLine = String.format("%.0f GeV", energy);
        font.setColor(LABEL_DIM);
        layout.setText(font, eLine);
        font.draw(batch, eLine, cx - layout.width * 0.5f, cy - 2f);
    }

    // ── helpers ────────────────────────────────────────────────────────

    /** IP1 (ATLAS), IP2 (ALICE), IP5 (CMS), IP8 (LHCb) are the 4 main experiments. */
    private static boolean isMainIP(int index) {
        return index == 0 || index == 1 || index == 4 || index == 7;
    }

    private static Color hsvToColor(float h, float s, float v, float a) {
        float c = v * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        int hi = (int) (h * 6f) % 6;
        switch (hi) {
            case 0:  r = c; g = x; b = 0; break;
            case 1:  r = x; g = c; b = 0; break;
            case 2:  r = 0; g = c; b = x; break;
            case 3:  r = 0; g = x; b = c; break;
            case 4:  r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return new Color(r + m, g + m, b + m, a);
    }
}
