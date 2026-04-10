package com.lhcsim.render.ring;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.lhcsim.physics.accelerator.AcceleratorElement;
import com.lhcsim.physics.accelerator.Lattice;
import com.lhcsim.physics.beam.TwissPropagator;

import java.util.List;

/**
 * Renders beta_x(s) and beta_y(s) functions along a lattice as filled
 * curves at the bottom of the ring view.
 * <p>
 * Uses a {@link ShapeRenderer} in Filled mode with cyan for beta_x
 * and orange for beta_y. Element-type icons are drawn along the
 * s-axis at their correct positions.
 */
public class BetaFunctionPlot {

    private static final Color BETA_X_COLOR = new Color(0x00D4FF88);
    private static final Color BETA_Y_COLOR = new Color(0xFF6B3588);
    private static final Color AXIS_COLOR = new Color(0x4477AAFF);
    private static final Color GRID_LINE_COLOR = new Color(0x1A2744FF);
    private static final Color QUAD_ICON_COLOR = new Color(0xCC3333FF);
    private static final Color DIPOLE_ICON_COLOR = new Color(0x336699FF);
    private static final Color RF_ICON_COLOR = new Color(0xFFCC00FF);
    private static final Color LABEL_COLOR = new Color(0x6688BBFF);

    private List<TwissPropagator.SamplePoint> samples;
    private double maxBeta;
    private double totalLength;
    private Lattice lattice;

    private final GlyphLayout layout = new GlyphLayout();

    /**
     * Updates the beta function data from a lattice.
     *
     * @param lattice the accelerator lattice
     * @param gamma   Lorentz gamma of the reference particle
     */
    public void update(Lattice lattice, double gamma) {
        this.lattice = lattice;
        try {
            this.samples = TwissPropagator.betaFunction(lattice, gamma);
        } catch (IllegalStateException e) {
            // Lattice is unstable — clear the plot
            this.samples = null;
            return;
        }
        this.totalLength = lattice.totalLength();

        maxBeta = 0;
        for (TwissPropagator.SamplePoint sp : samples) {
            maxBeta = Math.max(maxBeta, Math.max(sp.betaX(), sp.betaY()));
        }
        maxBeta *= 1.1; // 10% headroom
    }

    /**
     * Renders the beta function plot into a rectangular region.
     *
     * @param shapes   ShapeRenderer (caller manages begin/end for Filled)
     * @param batch    SpriteBatch for text labels (caller manages begin/end)
     * @param font     BitmapFont for labels
     * @param x0       left edge of the plot area (pixels)
     * @param y0       bottom edge (pixels)
     * @param plotW    width (pixels)
     * @param plotH    height (pixels)
     */
    public void render(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
                       float x0, float y0, float plotW, float plotH) {
        if (samples == null || samples.size() < 2 || maxBeta <= 0) {
            return;
        }

        // ── filled beta curves ──────────────────────────────────────
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Grid lines every 10 m along s
        shapes.setColor(GRID_LINE_COLOR);
        double gridStep = 10.0;
        if (totalLength > 200) gridStep = 50.0;
        if (totalLength > 1000) gridStep = 100.0;
        for (double s = gridStep; s < totalLength; s += gridStep) {
            float px = x0 + (float) (s / totalLength) * plotW;
            shapes.rectLine(px, y0, px, y0 + plotH, 1f);
        }

        // Beta_x (cyan, filled from baseline)
        drawFilledCurve(shapes, x0, y0, plotW, plotH, BETA_X_COLOR, true);

        // Beta_y (orange, filled from baseline)
        drawFilledCurve(shapes, x0, y0, plotW, plotH, BETA_Y_COLOR, false);

        // Element icons along the s-axis
        if (lattice != null) {
            drawElementIcons(shapes, x0, y0, plotW);
        }

        shapes.end();

        // ── axis lines ──────────────────────────────────────────────
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(AXIS_COLOR);
        shapes.line(x0, y0, x0 + plotW, y0); // s-axis
        shapes.line(x0, y0, x0, y0 + plotH); // beta-axis
        shapes.end();

        // ── text labels ─────────────────────────────────────────────
        batch.begin();
        font.setColor(LABEL_COLOR);

        // Y-axis label
        String maxLabel = String.format("%.1f m", maxBeta);
        layout.setText(font, maxLabel);
        font.draw(batch, maxLabel, x0 + 2, y0 + plotH - 2);

        // S-axis label
        String sLabel = String.format("s [0 - %.0f m]", totalLength);
        layout.setText(font, sLabel);
        font.draw(batch, sLabel, x0 + plotW - layout.width - 4, y0 + layout.height + 2);

        // Legend
        font.setColor(new Color(BETA_X_COLOR.r, BETA_X_COLOR.g, BETA_X_COLOR.b, 1f));
        font.draw(batch, "Bx", x0 + 4, y0 + plotH + layout.height + 4);
        font.setColor(new Color(BETA_Y_COLOR.r, BETA_Y_COLOR.g, BETA_Y_COLOR.b, 1f));
        font.draw(batch, "By", x0 + 30, y0 + plotH + layout.height + 4);

        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Returns the tooltip text for a given pixel x-position, or null if outside.
     */
    public String getTooltip(float pixelX, float plotX0, float plotW) {
        if (samples == null || lattice == null || samples.size() < 2) return null;
        float t = (pixelX - plotX0) / plotW;
        if (t < 0 || t > 1) return null;

        double s = t * totalLength;
        // Find closest sample
        TwissPropagator.SamplePoint closest = samples.get(0);
        for (TwissPropagator.SamplePoint sp : samples) {
            if (Math.abs(sp.s() - s) < Math.abs(closest.s() - s)) {
                closest = sp;
            }
        }

        // Find element at this s
        String elementName = "—";
        for (AcceleratorElement el : lattice.getElements()) {
            if (el.getSPosition() <= s && s <= el.getSPosition() + el.getLength()) {
                elementName = el.getName();
                break;
            }
        }

        return String.format("s=%.1f m  Bx=%.2f  By=%.2f  [%s]",
                closest.s(), closest.betaX(), closest.betaY(), elementName);
    }

    // ── internal helpers ────────────────────────────────────────────

    private void drawFilledCurve(ShapeRenderer shapes, float x0, float y0,
                                  float plotW, float plotH, Color color, boolean isBetaX) {
        shapes.setColor(color);
        for (int i = 0; i < samples.size() - 1; i++) {
            TwissPropagator.SamplePoint p0 = samples.get(i);
            TwissPropagator.SamplePoint p1 = samples.get(i + 1);

            float sx0 = x0 + (float) (p0.s() / totalLength) * plotW;
            float sx1 = x0 + (float) (p1.s() / totalLength) * plotW;
            double v0 = isBetaX ? p0.betaX() : p0.betaY();
            double v1 = isBetaX ? p1.betaX() : p1.betaY();
            float sy0 = y0 + (float) (v0 / maxBeta) * plotH;
            float sy1 = y0 + (float) (v1 / maxBeta) * plotH;

            // Two triangles forming a filled quad from baseline to curve
            shapes.triangle(sx0, y0, sx0, sy0, sx1, sy1);
            shapes.triangle(sx0, y0, sx1, sy1, sx1, y0);
        }
    }

    private void drawElementIcons(ShapeRenderer shapes, float x0, float y0, float plotW) {
        float iconH = 4f;
        for (AcceleratorElement el : lattice.getElements()) {
            float sx = x0 + (float) ((el.getSPosition() + el.getLength() / 2.0) / totalLength) * plotW;
            String type = el.getElementType();
            float halfW = Math.max(1f, (float) (el.getLength() / totalLength) * plotW * 0.5f);

            switch (type) {
                case "QUAD" -> {
                    shapes.setColor(QUAD_ICON_COLOR);
                    // Small vertical line for quad
                    shapes.rectLine(sx, y0 - iconH, sx, y0 + iconH, 1.5f);
                }
                case "DIPOLE" -> {
                    shapes.setColor(DIPOLE_ICON_COLOR);
                    // Small rectangle for dipole
                    shapes.rect(sx - halfW, y0 - iconH * 0.5f, halfW * 2, iconH);
                }
                case "RFCAV" -> {
                    shapes.setColor(RF_ICON_COLOR);
                    // Small square for RF
                    shapes.rect(sx - 2, y0 - 2, 4, 4);
                }
                default -> {
                    // Skip other types
                }
            }
        }
    }
}
