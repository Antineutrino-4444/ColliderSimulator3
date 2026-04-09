package com.lhcsim.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Application configuration POJO, loaded from {@code config.json}.
 * <p>
 * Fields: window size, fullscreen flag, target FPS, master volume placeholder
 * (unused per requirements), difficulty, colorblind mode, locale.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    private int windowWidth = 1280;
    private int windowHeight = 720;
    private boolean fullscreen = false;
    private int targetFps = 60;
    private float masterVolume = 1.0f;
    private String difficulty = "normal";
    private boolean colorblindMode = false;
    private String locale = "en";

    public AppConfig() {}

    // ── Getters & Setters ──────────────────────────────────────────

    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }

    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }

    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int targetFps) { this.targetFps = targetFps; }

    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isColorblindMode() { return colorblindMode; }
    public void setColorblindMode(boolean colorblindMode) { this.colorblindMode = colorblindMode; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}
