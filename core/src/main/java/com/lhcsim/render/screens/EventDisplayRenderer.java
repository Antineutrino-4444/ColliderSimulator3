package com.lhcsim.render.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.lhcsim.physics.particles.ReconstructedObject;
import com.lhcsim.physics.particles.ReconstructedObject.ObjectType;

import java.util.List;

/**
 * Draws a CMS-style circular cross-section (r-φ) event display.
 * Concentric detector layers with particle tracks curving in the magnetic field.
 */
public class EventDisplayRenderer {

    // Detector layer radii as fractions of the total display radius
    private static final float R_BEAMPIPE    = 0.03f;
    private static final float R_TRACKER     = 0.25f;
    private static final float R_ECAL_INNER  = 0.40f;
    private static final float R_ECAL_OUTER  = 0.55f;
    private static final float R_HCAL_INNER  = 0.55f;
    private static final float R_HCAL_OUTER  = 0.75f;
    private static final float R_SOLENOID    = 0.78f;
    private static final float R_MUON_INNER  = 0.85f;
    private static final float R_MUON_OUTER  = 1.00f;

    private static final int ECAL_SEGMENTS = 36;
    private static final int HCAL_SEGMENTS = 18;
    private static final int MUON_SEGMENTS = 12;
    private static final int CIRCLE_VERTS  = 80;

    // Particle colors
    private static final Color COL_MUON     = new Color(0x4488FFFF);
    private static final Color COL_ELECTRON = new Color(0x44FF44FF);
    private static final Color COL_PHOTON   = new Color(0xFFDD00FF);
    private static final Color COL_JET      = new Color(0xFF8844FF);
    private static final Color COL_MET      = new Color(0xFF44FFFF);
    private static final Color COL_TAU      = new Color(0xFF8844FF);

    private static final Color COL_LAYER    = new Color(0.25f, 0.25f, 0.30f, 1f);
    private static final Color COL_SEGMENT  = new Color(0.20f, 0.20f, 0.24f, 1f);
    private static final Color COL_BG       = new Color(0.06f, 0.06f, 0.10f, 1f);

    private static final float MAX_PT_GEV = 150f;

    private final GlyphLayout layout = new GlyphLayout();

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                       float centerX, float centerY, float radius,
                       List<ReconstructedObject> recoObjects, float animTime) {

        // --- Pass 1: Filled background & calorimeter deposits ---
        shapes.begin(ShapeType.Filled);
        drawBackground(shapes, centerX, centerY, radius);
        if (recoObjects != null) {
            drawCaloDeposits(shapes, centerX, centerY, radius, recoObjects);
        }
        shapes.end();

        // --- Pass 2: Line geometry (detector outlines, tracks) ---
        shapes.begin(ShapeType.Line);
        drawDetectorOutlines(shapes, centerX, centerY, radius);
        drawCaloSegments(shapes, centerX, centerY, radius);
        if (recoObjects != null) {
            for (ReconstructedObject obj : recoObjects) {
                drawRecoObject(shapes, centerX, centerY, radius, obj, animTime);
            }
        }
        shapes.end();

        // --- Pass 3: Text labels ---
        batch.begin();
        drawLabels(batch, font, centerX, centerY, radius, recoObjects);
        batch.end();
    }

    // ── Background ──────────────────────────────────────────────────

    private void drawBackground(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(COL_BG);
        fillCircle(s, cx, cy, r * R_MUON_OUTER);
    }

    // ── Detector layer outlines ─────────────────────────────────────

    private void drawDetectorOutlines(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(COL_LAYER);
        strokeCircle(s, cx, cy, r * R_BEAMPIPE);
        strokeCircle(s, cx, cy, r * R_TRACKER);
        strokeCircle(s, cx, cy, r * R_ECAL_INNER);
        strokeCircle(s, cx, cy, r * R_ECAL_OUTER);
        strokeCircle(s, cx, cy, r * R_HCAL_OUTER);
        strokeCircle(s, cx, cy, r * R_SOLENOID);
        strokeCircle(s, cx, cy, r * R_MUON_INNER);
        strokeCircle(s, cx, cy, r * R_MUON_OUTER);
    }

    // ── Calorimeter cell grid ───────────────────────────────────────

    private void drawCaloSegments(ShapeRenderer s, float cx, float cy, float r) {
        s.setColor(COL_SEGMENT);
        drawRadialLines(s, cx, cy, r, ECAL_SEGMENTS, R_ECAL_INNER, R_ECAL_OUTER);
        drawRadialLines(s, cx, cy, r, HCAL_SEGMENTS, R_HCAL_INNER, R_HCAL_OUTER);
        drawRadialLines(s, cx, cy, r, MUON_SEGMENTS, R_MUON_INNER, R_MUON_OUTER);
    }

    private void drawRadialLines(ShapeRenderer s, float cx, float cy, float r,
                                 int segments, float rInner, float rOuter) {
        for (int i = 0; i < segments; i++) {
            float angle = MathUtils.PI2 * i / segments;
            float cos = MathUtils.cos(angle), sin = MathUtils.sin(angle);
            s.line(cx + cos * r * rInner, cy + sin * r * rInner,
                   cx + cos * r * rOuter, cy + sin * r * rOuter);
        }
    }

    private void drawCaloDeposits(ShapeRenderer s, float cx, float cy, float r,
                                  List<ReconstructedObject> objects) {
        for (ReconstructedObject obj : objects) {
            ObjectType t = obj.getType();
            if (t == ObjectType.MET) continue;
            float phi = (float) obj.getPhi();
            float fraction = Math.min((float) obj.getEnergy() / MAX_PT_GEV, 1f);
            Color col = colorFor(t);
            s.setColor(new Color(col.r, col.g, col.b, 0.45f));
            boolean hadronic = (t == ObjectType.JET || t == ObjectType.BJET || t == ObjectType.TAU);
            float ecalFrac = hadronic ? fraction * 0.3f : fraction;
            drawWedge(s, cx, cy, r * R_ECAL_INNER,
                    r * R_ECAL_INNER + r * (R_ECAL_OUTER - R_ECAL_INNER) * ecalFrac,
                    phi, MathUtils.PI2 / ECAL_SEGMENTS);
            float hcalFrac = hadronic ? fraction : fraction * 0.15f;
            drawWedge(s, cx, cy, r * R_HCAL_INNER,
                    r * R_HCAL_INNER + r * (R_HCAL_OUTER - R_HCAL_INNER) * hcalFrac,
                    phi, MathUtils.PI2 / HCAL_SEGMENTS);
        }
    }

    private void drawWedge(ShapeRenderer s, float cx, float cy,
                           float rInner, float rOuter, float phi, float span) {
        float a0 = phi - span * 0.5f, a1 = phi + span * 0.5f;
        for (int i = 0; i < 6; i++) {
            float t0 = a0 + (a1 - a0) * i / 6;
            float t1 = a0 + (a1 - a0) * (i + 1) / 6;
            float c0 = MathUtils.cos(t0), s0 = MathUtils.sin(t0);
            float c1 = MathUtils.cos(t1), s1 = MathUtils.sin(t1);
            s.triangle(cx + c0 * rInner, cy + s0 * rInner,
                       cx + c0 * rOuter, cy + s0 * rOuter,
                       cx + c1 * rOuter, cy + s1 * rOuter);
            s.triangle(cx + c0 * rInner, cy + s0 * rInner,
                       cx + c1 * rOuter, cy + s1 * rOuter,
                       cx + c1 * rInner, cy + s1 * rInner);
        }
    }

    // ── Particle tracks & MET arrow ─────────────────────────────────

    private void drawRecoObject(ShapeRenderer s, float cx, float cy, float r,
                                ReconstructedObject obj, float animTime) {
        ObjectType t = obj.getType();
        s.setColor(colorFor(t));

        float phi = (float) obj.getPhi();
        float pT = (float) obj.getPT();
        int charge = obj.getCharge();

        if (t == ObjectType.MET) {
            drawMetArrow(s, cx, cy, r, phi, pT, animTime);
            return;
        }

        float maxR = trackMaxRadius(t, r);

        if (t == ObjectType.PHOTON) {
            drawPhotonTrack(s, cx, cy, phi, maxR);
        } else if (t == ObjectType.JET || t == ObjectType.BJET || t == ObjectType.TAU) {
            drawJetCone(s, cx, cy, r, phi, pT);
        } else {
            drawChargedTrack(s, cx, cy, phi, pT, charge, maxR);
        }
    }

    private void drawChargedTrack(ShapeRenderer s, float cx, float cy,
                                  float phi, float pT, int charge, float maxR) {
        // Sagitta-based curvature: low pT → more bending
        float curvature = (charge != 0) ? charge * 0.12f / Math.max(pT / MAX_PT_GEV, 0.05f) : 0f;
        int steps = 50;
        float prevX = cx, prevY = cy;
        for (int i = 1; i <= steps; i++) {
            float frac = (float) i / steps;
            float dist = maxR * frac;
            float bend = curvature * frac * frac;
            float angle = phi + bend;
            float x = cx + MathUtils.cos(angle) * dist;
            float y = cy + MathUtils.sin(angle) * dist;
            s.line(prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }
    }

    private void drawPhotonTrack(ShapeRenderer s, float cx, float cy,
                                 float phi, float maxR) {
        // Photon: straight dashed line (no magnetic bending)
        int dashes = 12;
        float cos = MathUtils.cos(phi);
        float sin = MathUtils.sin(phi);
        for (int i = 0; i < dashes; i++) {
            float r0 = maxR * i / dashes;
            float r1 = maxR * (i + 0.6f) / dashes;
            s.line(cx + cos * r0, cy + sin * r0, cx + cos * r1, cy + sin * r1);
        }
    }

    private void drawJetCone(ShapeRenderer s, float cx, float cy, float r,
                             float phi, float pT) {
        float width = MathUtils.clamp(pT / MAX_PT_GEV, 0.05f, 0.4f) * 0.25f;
        float outerR = r * R_HCAL_OUTER;
        float cos0 = MathUtils.cos(phi - width);
        float sin0 = MathUtils.sin(phi - width);
        float cos1 = MathUtils.cos(phi + width);
        float sin1 = MathUtils.sin(phi + width);

        s.line(cx, cy, cx + cos0 * outerR, cy + sin0 * outerR);
        s.line(cx, cy, cx + cos1 * outerR, cy + sin1 * outerR);
        // Arc at the cone mouth
        int arcSteps = 10;
        for (int i = 0; i < arcSteps; i++) {
            float a0 = phi - width + 2f * width * i / arcSteps;
            float a1 = phi - width + 2f * width * (i + 1) / arcSteps;
            s.line(cx + MathUtils.cos(a0) * outerR, cy + MathUtils.sin(a0) * outerR,
                   cx + MathUtils.cos(a1) * outerR, cy + MathUtils.sin(a1) * outerR);
        }
    }

    private void drawMetArrow(ShapeRenderer s, float cx, float cy, float r,
                              float phi, float pT, float animTime) {
        float len = r * R_HCAL_OUTER * Math.min(pT / MAX_PT_GEV, 1f) * 0.9f;
        float cos = MathUtils.cos(phi);
        float sin = MathUtils.sin(phi);
        // Dashed line
        int dashes = 10;
        for (int i = 0; i < dashes; i++) {
            float dashPhase = (i + animTime * 3f) % dashes;
            float r0 = len * dashPhase / dashes;
            float r1 = len * (dashPhase + 0.55f) / dashes;
            if (r1 > len) r1 = len;
            if (r0 > len) continue;
            s.line(cx + cos * r0, cy + sin * r0, cx + cos * r1, cy + sin * r1);
        }
        // Arrowhead
        float ax = cx + cos * len;
        float ay = cy + sin * len;
        float headLen = r * 0.04f;
        float headAngle = 0.35f;
        s.line(ax, ay,
               ax - MathUtils.cos(phi - headAngle) * headLen,
               ay - MathUtils.sin(phi - headAngle) * headLen);
        s.line(ax, ay,
               ax - MathUtils.cos(phi + headAngle) * headLen,
               ay - MathUtils.sin(phi + headAngle) * headLen);
    }

    // ── Text labels ─────────────────────────────────────────────────

    private void drawLabels(SpriteBatch batch, BitmapFont font,
                            float cx, float cy, float r,
                            List<ReconstructedObject> objects) {
        font.setColor(Color.WHITE);
        layout.setText(font, "CMS Event Display");
        font.draw(batch, "CMS Event Display",
                cx - layout.width * 0.5f, cy + r + layout.height + 4f);

        font.setColor(COL_LAYER);
        String[] labels = {"\u03c6=0", "\u03c6=\u03c0", "\u03c6=\u03c0/2", "\u03c6=-\u03c0/2"};
        float[][] off = {{r * 1.05f, 0}, {-r * 1.05f, 0}, {0, r * 1.05f}, {0, -r * 1.05f}};
        for (int i = 0; i < 4; i++) {
            layout.setText(font, labels[i]);
            font.draw(batch, labels[i],
                    cx + off[i][0] - layout.width * 0.5f,
                    cy + off[i][1] + layout.height * 0.5f);
        }

        if (objects != null && !objects.isEmpty()) {
            font.setColor(new Color(0.6f, 0.6f, 0.65f, 1f));
            String info = objectSummary(objects);
            layout.setText(font, info);
            font.draw(batch, info, cx - layout.width * 0.5f, cy - r - 4f);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Color colorFor(ObjectType t) {
        switch (t) {
            case MUON:     return COL_MUON;
            case ELECTRON: return COL_ELECTRON;
            case PHOTON:   return COL_PHOTON;
            case JET:      return COL_JET;
            case BJET:     return COL_JET;
            case TAU:      return COL_TAU;
            case MET:      return COL_MET;
            default:       return Color.GRAY;
        }
    }

    private float trackMaxRadius(ObjectType t, float r) {
        switch (t) {
            case MUON:     return r * R_MUON_OUTER;
            case ELECTRON: return r * R_ECAL_INNER;
            case PHOTON:   return r * R_ECAL_INNER;
            default:       return r * R_TRACKER;
        }
    }

    private void strokeCircle(ShapeRenderer s, float cx, float cy, float r) {
        s.circle(cx, cy, r, CIRCLE_VERTS);
    }

    private void fillCircle(ShapeRenderer s, float cx, float cy, float r) {
        s.circle(cx, cy, r, CIRCLE_VERTS);
    }

    private String objectSummary(List<ReconstructedObject> objects) {
        int mu = 0, el = 0, ph = 0, jet = 0;
        boolean hasMet = false;
        for (ReconstructedObject o : objects) {
            switch (o.getType()) {
                case MUON:     mu++;  break;
                case ELECTRON: el++;  break;
                case PHOTON:   ph++;  break;
                case JET: case BJET: case TAU: jet++; break;
                case MET:      hasMet = true; break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (mu > 0)  sb.append(mu).append("\u03bc ");
        if (el > 0)  sb.append(el).append("e ");
        if (ph > 0)  sb.append(ph).append("\u03b3 ");
        if (jet > 0) sb.append(jet).append("jet ");
        if (hasMet)  sb.append("MET");
        return sb.toString().trim();
    }
}
