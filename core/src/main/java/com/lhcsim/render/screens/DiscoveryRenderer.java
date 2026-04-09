package com.lhcsim.render.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.lhcsim.physics.collision.EventGenerator.GeneratedParticle;
import com.lhcsim.physics.collision.EventGenerator.PhysicsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Discovery Workbench: invariant-mass histograms where particle
 * physics discoveries emerge as bumps above smooth backgrounds.  As the player
 * collects integrated luminosity, signal peaks gradually rise from the
 * background until they cross the 5σ discovery threshold.
 */
public class DiscoveryRenderer {

    // ── design-doc palette ──────────────────────────────────────────────
    private static final Color COL_PANEL_BG  = new Color(0x0A0E1FFF);
    private static final Color COL_HIST_BG   = new Color(0x1A3366FF);
    private static final Color COL_CYAN      = new Color(0x00D4FFFF);
    private static final Color COL_ORANGE    = new Color(0xFF6B35FF);
    private static final Color COL_GREEN     = new Color(0x30D158FF);
    private static final Color COL_RED       = new Color(0xFF3B30FF);
    private static final Color COL_GRID      = new Color(0.15f, 0.15f, 0.20f, 1f);
    private static final Color COL_AXIS      = new Color(0.5f, 0.5f, 0.55f, 1f);
    private static final Color COL_YELLOW    = new Color(1f, 1f, 0.3f, 1f);

    private static final float DISCOVERY_SIGMA = 5.0f;
    private static final float EVIDENCE_SIGMA  = 3.0f;

    // Background model tuning constants
    private static final double BG_DIPHOTON_NORM  = 0.6;
    private static final double BG_DIPHOTON_SLOPE = 0.025;
    private static final double BG_ZZ_NORM        = 0.3;
    private static final double BG_ZZ_FALLOFF     = 0.5;
    private static final double Z_BOSON_WIDTH_GEV = 2.495;
    private static final double BG_DY_PEAK_NORM   = 0.08;
    private static final double BG_DY_FLAT        = 0.02;

    // ── histogram definitions ───────────────────────────────────────────
    private static final int NUM_HISTOGRAMS = 3;

    private final HistogramDef[] histograms = {
        new HistogramDef("m_yy", "H->yy",
                         80, 180, 50, 125, 1.7, "Higgs_diphoton"),
        new HistogramDef("m_4l", "H->ZZ->4l",
                         70, 200, 65, 125, 2.0, "Higgs_ZZ_4l"),
        new HistogramDef("m_mumu", "Z->mumu",
                         60, 120, 60, 91.2, 2.5, "Drell-Yan_mumu"),
    };

    private final List<String> claimedDiscoveries = new ArrayList<>();
    private final GlyphLayout layout = new GlyphLayout();

    // ── inner histogram state ───────────────────────────────────────────
    private static final class HistogramDef {
        final String axisLabel, discoveryName, processMatch;
        final double lo, hi, peakMass, peakWidth;
        final int nBins;
        final double[] signal, background;
        double significance;
        boolean claimed;

        HistogramDef(String axisLabel, String discoveryName,
                     double lo, double hi, int nBins,
                     double peakMass, double peakWidth, String processMatch) {
            this.axisLabel = axisLabel; this.discoveryName = discoveryName;
            this.lo = lo; this.hi = hi; this.nBins = nBins;
            this.peakMass = peakMass; this.peakWidth = peakWidth;
            this.processMatch = processMatch;
            this.signal = new double[nBins]; this.background = new double[nBins];
        }
        double binWidth() { return (hi - lo) / nBins; }
        int binFor(double mass) {
            int b = (int) ((mass - lo) / binWidth());
            return (b >= 0 && b < nBins) ? b : -1;
        }
    }

    // ── public API ──────────────────────────────────────────────────────
    public DiscoveryRenderer() {}

    /** Accumulate new events into the histograms. */
    public void accumulateEvents(List<PhysicsEvent> events) {
        if (events == null) return;
        for (PhysicsEvent ev : events) {
            for (HistogramDef hd : histograms) {
                addEventToHistogram(hd, ev);
            }
        }
        for (HistogramDef hd : histograms) {
            hd.significance = computeSignificance(hd);
        }
    }

    public boolean hasClaimableDiscovery() {
        for (HistogramDef hd : histograms) {
            if (!hd.claimed && hd.significance >= DISCOVERY_SIGMA) return true;
        }
        return false;
    }

    public String claimDiscovery() {
        HistogramDef best = null;
        for (HistogramDef hd : histograms) {
            if (!hd.claimed && hd.significance >= DISCOVERY_SIGMA) {
                if (best == null || hd.significance > best.significance) best = hd;
            }
        }
        if (best == null) return null;
        best.claimed = true;
        claimedDiscoveries.add(best.discoveryName);
        return best.discoveryName;
    }

    public List<String> getClaimedDiscoveries() {
        return new ArrayList<>(claimedDiscoveries);
    }

    /** Render the full Discovery Workbench panel. */
    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                       BitmapFont smallFont,
                       float x, float y, float w, float h, float animTime,
                       double integratedLumiFb, String eraName) {
        float pad = 8f;
        float histW = (w - pad * 4) / NUM_HISTOGRAMS;
        float headerH = 28f;
        float histH = h - headerH - pad * 2;

        // ── filled pass ─────────────────────────────────────────────────
        shapes.begin(ShapeType.Filled);
        shapes.setColor(COL_PANEL_BG);
        shapes.rect(x, y, w, h);
        for (int i = 0; i < NUM_HISTOGRAMS; i++) {
            drawHistogramFilled(shapes, histograms[i],
                    x + pad + i * (histW + pad), y + pad, histW, histH);
        }
        shapes.end();

        // ── line pass ───────────────────────────────────────────────────
        shapes.begin(ShapeType.Line);
        for (int i = 0; i < NUM_HISTOGRAMS; i++) {
            drawHistogramLines(shapes, histograms[i],
                    x + pad + i * (histW + pad), y + pad, histW, histH, animTime);
        }
        shapes.end();

        // ── text pass ───────────────────────────────────────────────────
        batch.begin();
        drawHeader(batch, font, x, y, w, h, integratedLumiFb, eraName);
        for (int i = 0; i < NUM_HISTOGRAMS; i++) {
            drawHistogramLabels(batch, font, smallFont, histograms[i],
                    x + pad + i * (histW + pad), y + pad, histW, histH, animTime);
        }
        batch.end();
    }

    // ── event accumulation ──────────────────────────────────────────────
    private void addEventToHistogram(HistogramDef hd, PhysicsEvent ev) {
        boolean matchProcess = ev.processName() != null
                && ev.processName().contains(hd.processMatch);
        double mass = reconstructMass(hd, ev);
        if (mass > 0) {
            int bin = hd.binFor(mass);
            if (bin >= 0) {
                if (matchProcess) hd.signal[bin] += 1.0;
                hd.background[bin] += backgroundRate(hd, mass);
            }
        }
    }

    private double reconstructMass(HistogramDef hd, PhysicsEvent ev) {
        List<GeneratedParticle> finals = new ArrayList<>();
        for (GeneratedParticle p : ev.particles()) {
            if (p.isFinalState()) finals.add(p);
        }
        if (hd.processMatch.contains("diphoton")) return diparticleMass(finals, 22, 22);
        if (hd.processMatch.contains("4l")) return fourLeptonMass(finals);
        if (hd.processMatch.contains("mumu")) return diparticleMass(finals, 13, -13);
        return -1;
    }

    private double diparticleMass(List<GeneratedParticle> parts, int pdg1, int pdg2) {
        GeneratedParticle p1 = null, p2 = null;
        for (GeneratedParticle p : parts) {
            if (p1 == null && p.pdgId() == pdg1) p1 = p;
            else if (p2 == null && p.pdgId() == pdg2) p2 = p;
        }
        if (p1 == null || p2 == null) return -1;
        return invariantMass(p1, p2);
    }

    private double fourLeptonMass(List<GeneratedParticle> parts) {
        double px = 0, py = 0, pz = 0, e = 0; int count = 0;
        for (GeneratedParticle p : parts) {
            int id = Math.abs(p.pdgId());
            if ((id == 11 || id == 13) && count < 4) {
                px += p.px(); py += p.py(); pz += p.pz(); e += p.energy(); count++;
            }
        }
        if (count < 4) return -1;
        double m2 = e * e - px * px - py * py - pz * pz;
        return m2 > 0 ? Math.sqrt(m2) : -1;
    }

    private double invariantMass(GeneratedParticle a, GeneratedParticle b) {
        double totalE  = a.energy() + b.energy();
        double totalPx = a.px() + b.px();
        double totalPy = a.py() + b.py();
        double totalPz = a.pz() + b.pz();
        double m2 = totalE * totalE - totalPx * totalPx - totalPy * totalPy - totalPz * totalPz;
        return m2 > 0 ? Math.sqrt(m2) : -1;
    }

    /** Expected background rate per event at a given mass value. */
    private double backgroundRate(HistogramDef hd, double mass) {
        if (hd.processMatch.contains("diphoton")) {
            return BG_DIPHOTON_NORM * Math.exp(-(mass - hd.lo) * BG_DIPHOTON_SLOPE);
        } else if (hd.processMatch.contains("4l")) {
            double t = (mass - hd.lo) / (hd.hi - hd.lo);
            return BG_ZZ_NORM * (1.0 - BG_ZZ_FALLOFF * t) * (1.0 - BG_ZZ_FALLOFF * t);
        }
        double halfW = Z_BOSON_WIDTH_GEV / 2.0;
        double denom = (mass - 91.2) * (mass - 91.2) + halfW * halfW;
        return BG_DY_PEAK_NORM * (halfW * halfW) / denom + BG_DY_FLAT;
    }

    // ── significance ────────────────────────────────────────────────────
    private double computeSignificance(HistogramDef hd) {
        double windowLo = hd.peakMass - 2 * hd.peakWidth;
        double windowHi = hd.peakMass + 2 * hd.peakWidth;
        double sig = 0, bg = 0;
        for (int b = 0; b < hd.nBins; b++) {
            double center = hd.lo + (b + 0.5) * hd.binWidth();
            if (center >= windowLo && center <= windowHi) {
                sig += hd.signal[b]; bg += hd.background[b];
            }
        }
        return bg > 0 ? sig / Math.sqrt(bg) : 0;
    }

    // ── histogram rendering (filled) ────────────────────────────────────
    private void drawHistogramFilled(ShapeRenderer s, HistogramDef hd,
                                     float hx, float hy, float hw, float hh) {
        float axisMargin = 20f;
        float plotX = hx + axisMargin;
        float plotY = hy + axisMargin;
        float plotW = hw - axisMargin - 4f;
        float plotH = hh - axisMargin - 16f;

        // panel background
        s.setColor(new Color(0.04f, 0.05f, 0.10f, 1f));
        s.rect(hx, hy, hw, hh);

        double maxVal = 1;
        for (int b = 0; b < hd.nBins; b++) {
            maxVal = Math.max(maxVal, hd.signal[b] + hd.background[b]);
        }

        float binW = plotW / hd.nBins;

        // background fill
        s.setColor(COL_HIST_BG);
        for (int b = 0; b < hd.nBins; b++) {
            float barH = (float) (hd.background[b] / maxVal) * plotH;
            s.rect(plotX + b * binW, plotY, binW - 1f, barH);
        }

        // signal+background fill (slightly lighter)
        s.setColor(new Color(COL_HIST_BG.r * 1.6f, COL_HIST_BG.g * 1.6f,
                             COL_HIST_BG.b * 1.3f, 1f));
        for (int b = 0; b < hd.nBins; b++) {
            float bgH = (float) (hd.background[b] / maxVal) * plotH;
            float totH = (float) ((hd.signal[b] + hd.background[b]) / maxVal) * plotH;
            if (totH > bgH) {
                s.rect(plotX + b * binW, plotY + bgH, binW - 1f, totH - bgH);
            }
        }
    }

    // ── histogram rendering (lines) ─────────────────────────────────────
    private void drawHistogramLines(ShapeRenderer s, HistogramDef hd,
                                    float hx, float hy, float hw, float hh,
                                    float animTime) {
        float axisMargin = 20f;
        float plotX = hx + axisMargin;
        float plotY = hy + axisMargin;
        float plotW = hw - axisMargin - 4f;
        float plotH = hh - axisMargin - 16f;

        double maxVal = 1;
        for (int b = 0; b < hd.nBins; b++) {
            maxVal = Math.max(maxVal, hd.signal[b] + hd.background[b]);
        }

        float binW = plotW / hd.nBins;

        // grid lines (horizontal)
        s.setColor(COL_GRID);
        for (int g = 1; g <= 4; g++) {
            float gy = plotY + plotH * g / 4f;
            s.line(plotX, gy, plotX + plotW, gy);
        }

        // axes
        s.setColor(COL_AXIS);
        s.line(plotX, plotY, plotX + plotW, plotY);
        s.line(plotX, plotY, plotX, plotY + plotH);

        // signal+background outline (cyan)
        s.setColor(COL_CYAN);
        for (int b = 0; b < hd.nBins; b++) {
            float totH = (float) ((hd.signal[b] + hd.background[b]) / maxVal) * plotH;
            float bx = plotX + b * binW;
            s.line(bx, plotY, bx, plotY + totH);
            s.line(bx, plotY + totH, bx + binW - 1f, plotY + totH);
            s.line(bx + binW - 1f, plotY + totH, bx + binW - 1f, plotY);
        }

        // signal-only dashed line (orange)
        s.setColor(COL_ORANGE);
        for (int b = 0; b < hd.nBins - 1; b++) {
            float y1 = plotY + (float) (hd.signal[b] / maxVal) * plotH;
            float y2 = plotY + (float) (hd.signal[b + 1] / maxVal) * plotH;
            float x1 = plotX + (b + 0.5f) * binW;
            float x2 = plotX + (b + 1.5f) * binW;
            // dashed: draw only every other segment
            if (b % 2 == 0) {
                s.line(x1, y1, x2, y2);
            }
        }

        // 5σ glow indicator
        if (!hd.claimed && hd.significance >= DISCOVERY_SIGMA) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(animTime * 4f);
            s.setColor(new Color(COL_GREEN.r, COL_GREEN.g, COL_GREEN.b, 0.15f * pulse));
            float peakBin = (float) ((hd.peakMass - hd.lo) / hd.binWidth());
            float glowX = plotX + peakBin * binW - binW * 3;
            s.line(glowX, plotY, glowX, plotY + plotH);
            s.line(glowX + binW * 6, plotY, glowX + binW * 6, plotY + plotH);
        }
    }

    // ── histogram labels (text) ─────────────────────────────────────────
    private void drawHistogramLabels(SpriteBatch batch, BitmapFont font,
                                     BitmapFont smallFont, HistogramDef hd,
                                     float hx, float hy, float hw, float hh,
                                     float animTime) {
        float axisMargin = 20f;
        float plotX = hx + axisMargin;
        float plotY = hy + axisMargin;
        float plotW = hw - axisMargin - 4f;
        float plotH = hh - axisMargin - 16f;

        // title
        font.setColor(Color.WHITE);
        layout.setText(font, hd.axisLabel + " (GeV)");
        font.draw(batch, hd.axisLabel + " (GeV)",
                  plotX + plotW * 0.5f - layout.width * 0.5f,
                  hy + hh - 2f);

        // axis range labels
        smallFont.setColor(COL_AXIS);
        smallFont.draw(batch, String.valueOf((int) hd.lo), plotX, plotY - 2f);
        String hiLabel = String.valueOf((int) hd.hi);
        layout.setText(smallFont, hiLabel);
        smallFont.draw(batch, hiLabel, plotX + plotW - layout.width, plotY - 2f);

        // discovery name
        smallFont.setColor(COL_CYAN);
        layout.setText(smallFont, hd.discoveryName);
        smallFont.draw(batch, hd.discoveryName,
                       plotX + plotW - layout.width - 2f,
                       plotY + plotH - 2f);

        // significance label
        String sigText = String.format("sig = %.1f", hd.significance);
        Color sigCol;
        if (hd.significance >= DISCOVERY_SIGMA) {
            sigCol = COL_GREEN;
        } else if (hd.significance >= EVIDENCE_SIGMA) {
            sigCol = COL_YELLOW;
        } else {
            sigCol = Color.WHITE;
        }
        font.setColor(sigCol);
        layout.setText(font, sigText);
        font.draw(batch, sigText, plotX + 4f, plotY + plotH - 2f);

        // "5σ DISCOVERY!" flash
        if (!hd.claimed && hd.significance >= DISCOVERY_SIGMA) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(animTime * 5f);
            font.setColor(new Color(COL_GREEN.r, COL_GREEN.g, COL_GREEN.b, pulse));
            String disc = "5sig DISCOVERY!";
            layout.setText(font, disc);
            font.draw(batch, disc,
                      plotX + plotW * 0.5f - layout.width * 0.5f,
                      plotY + plotH * 0.5f + layout.height * 0.5f);
        }

        // "CLAIMED" badge
        if (hd.claimed) {
            font.setColor(new Color(COL_GREEN.r, COL_GREEN.g, COL_GREEN.b, 0.6f));
            String cl = "CLAIMED";
            layout.setText(font, cl);
            font.draw(batch, cl,
                      plotX + plotW * 0.5f - layout.width * 0.5f,
                      plotY + plotH * 0.5f + layout.height * 0.5f);
        }

        // "CLAIM DISCOVERY" button area
        if (!hd.claimed && hd.significance >= DISCOVERY_SIGMA) {
            float btnW = Math.min(hw * 0.7f, 120f);
            float btnH = 18f;
            float btnX = plotX + plotW * 0.5f - btnW * 0.5f;
            float btnY = hy + 2f;
            float glow = 0.6f + 0.4f * MathUtils.sin(animTime * 3f);
            font.setColor(new Color(COL_CYAN.r * glow, COL_CYAN.g * glow,
                                    COL_CYAN.b * glow, 1f));
            String btn = "CLAIM DISCOVERY";
            layout.setText(font, btn);
            font.draw(batch, btn,
                      btnX + btnW * 0.5f - layout.width * 0.5f,
                      btnY + btnH * 0.5f + layout.height * 0.5f);
        }
    }

    // ── header ──────────────────────────────────────────────────────────
    private void drawHeader(SpriteBatch batch, BitmapFont font,
                            float x, float y, float w, float h,
                            double integratedLumiFb, String eraName) {
        font.setColor(COL_CYAN);
        String title = "Discovery Workbench";
        layout.setText(font, title);
        font.draw(batch, title, x + 10f, y + h - 4f);

        font.setColor(COL_AXIS);
        String lumi = String.format("Int.L = %.1f fb^-1  %s",
                                    integratedLumiFb, eraName);
        layout.setText(font, lumi);
        font.draw(batch, lumi, x + w - layout.width - 10f, y + h - 4f);
    }
}
