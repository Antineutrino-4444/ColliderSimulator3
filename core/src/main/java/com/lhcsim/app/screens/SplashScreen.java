package com.lhcsim.app.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.lhcsim.app.LhcSimGame;
import com.lhcsim.render.ui.Palette;

/**
 * Splash screen shown at launch.  Displays the title for 1.2 s, then
 * fades out and transitions to the {@link com.lhcsim.render.screens.MainMenuScreen}.
 */
public class SplashScreen extends ScreenAdapter {

    private static final float SPLASH_DURATION = 1.2f;
    private static final float FADE_DURATION   = 0.4f;

    private final LhcSimGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private float elapsed;

    public SplashScreen(LhcSimGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        font.getData().setScale(3.0f);
        layout = new GlyphLayout();
        elapsed = 0;
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Background
        Gdx.gl.glClearColor(Palette.BG_DARK.r, Palette.BG_DARK.g, Palette.BG_DARK.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Compute alpha for fade-out
        float alpha = 1f;
        if (elapsed > SPLASH_DURATION) {
            alpha = 1f - Math.min(1f, (elapsed - SPLASH_DURATION) / FADE_DURATION);
        }

        // Draw title
        String title = "LHC Simulator";
        font.setColor(new Color(1f, 1f, 1f, alpha));
        layout.setText(font, title);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch.begin();
        font.draw(batch, title,
                (w - layout.width) / 2f,
                (h + layout.height) / 2f);
        batch.end();

        // Transition after splash + fade
        if (elapsed >= SPLASH_DURATION + FADE_DURATION) {
            game.setScreen(new com.lhcsim.render.screens.MainMenuScreen(game));
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
