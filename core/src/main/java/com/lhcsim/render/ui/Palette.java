package com.lhcsim.render.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Design-doc colour palette (§9.1) as named constants.
 * <p>
 * All colours match the CERN control-room aesthetic: dark navy backgrounds,
 * bright accent hues for status indicators.
 */
public final class Palette {

    private Palette() { /* constants only */ }

    // ── Backgrounds ────────────────────────────────────────────────
    public static final Color BG_DARK      = new Color(0x0A / 255f, 0x0E / 255f, 0x1F / 255f, 1f);
    public static final Color PANEL_BG     = new Color(0x10 / 255f, 0x18 / 255f, 0x30 / 255f, 1f);
    public static final Color PANEL_BORDER = new Color(0x20 / 255f, 0x40 / 255f, 0x70 / 255f, 1f);

    // ── Accents ────────────────────────────────────────────────────
    public static final Color ACCENT_CYAN   = new Color(0x00 / 255f, 0xD4 / 255f, 0xFF / 255f, 1f);
    public static final Color ACCENT_GREEN  = new Color(0x30 / 255f, 0xD1 / 255f, 0x58 / 255f, 1f);
    public static final Color ACCENT_YELLOW = new Color(1.0f, 0.85f, 0.2f, 1f);
    public static final Color ACCENT_RED    = new Color(0xFF / 255f, 0x3B / 255f, 0x30 / 255f, 1f);
    public static final Color ACCENT_ORANGE = new Color(0xFF / 255f, 0x6B / 255f, 0x35 / 255f, 1f);

    // ── Text ───────────────────────────────────────────────────────
    public static final Color TEXT_BRIGHT = Color.WHITE.cpy();
    public static final Color TEXT_DIM    = new Color(0.5f, 0.55f, 0.65f, 1f);
    public static final Color TEXT_INFO   = new Color(0.6f, 0.8f, 1.0f, 1f);

    // ── Tabs ───────────────────────────────────────────────────────
    public static final Color TAB_ACTIVE   = ACCENT_CYAN;
    public static final Color TAB_INACTIVE = new Color(0.25f, 0.30f, 0.40f, 1f);

    // ── Buttons ────────────────────────────────────────────────────
    public static final Color BTN_NORMAL = new Color(0.15f, 0.3f, 0.6f, 1f);
    public static final Color BTN_HOVER  = new Color(0.2f, 0.4f, 0.8f, 1f);
}
