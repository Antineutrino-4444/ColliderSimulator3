package com.lhcsim.app;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.lhcsim.render.LHCSimGame;

/**
 * Desktop entry-point for the LHC Simulator.
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("LHC Simulator");
        config.setWindowedMode(1600, 900);
        config.setForegroundFPS(60);
        config.useVsync(true);
        config.setInitialBackgroundColor(
                new com.badlogic.gdx.graphics.Color(0x0A / 255f, 0x0E / 255f, 0x1F / 255f, 1f));

        new Lwjgl3Application(new LHCSimGame(), config);
    }
}
