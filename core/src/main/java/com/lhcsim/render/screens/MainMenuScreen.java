package com.lhcsim.render.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.lhcsim.render.LHCSimGame;

/**
 * Main menu screen displayed when the game starts.
 * Shows the title and basic status information.
 */
public class MainMenuScreen extends ScreenAdapter {

    private static final Color BG_COLOR = new Color(0x0A / 255f, 0x0E / 255f, 0x1F / 255f, 1f);

    private final LHCSimGame game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont infoFont;
    private GlyphLayout layout;

    public MainMenuScreen(LHCSimGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        layout = new GlyphLayout();

        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(3.0f);

        infoFont = new BitmapFont();
        infoFont.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));
        infoFont.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, BG_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch.begin();

        // Title
        String title = "LHC Simulator";
        layout.setText(titleFont, title);
        titleFont.draw(batch, title,
                (w - layout.width) / 2f,
                h * 0.7f);

        // Subtitle
        String subtitle = "Particle Physics Control Room";
        layout.setText(infoFont, subtitle);
        infoFont.draw(batch, subtitle,
                (w - layout.width) / 2f,
                h * 0.7f - 60);

        // Status info
        int particleCount = game.getParticleDatabase() != null
                ? game.getParticleDatabase().size() : 0;
        int processCount = game.getCrossSectionTable() != null
                ? game.getCrossSectionTable().getAllProcesses().size() : 0;

        String status = String.format("Loaded %d particles, %d processes", particleCount, processCount);
        layout.setText(infoFont, status);
        infoFont.draw(batch, status,
                (w - layout.width) / 2f,
                h * 0.4f);

        // Instructions
        infoFont.setColor(new Color(0.5f, 0.5f, 0.6f, 1.0f));
        String instructions = "Systems initialised - simulation engine ready";
        layout.setText(infoFont, instructions);
        infoFont.draw(batch, instructions,
                (w - layout.width) / 2f,
                h * 0.25f);
        infoFont.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));

        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (infoFont != null) infoFont.dispose();
    }
}
