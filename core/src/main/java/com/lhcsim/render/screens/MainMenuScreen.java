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
import com.lhcsim.app.LhcSimGame;

/**
 * Main menu screen displayed when the game starts.
 * Shows the title and a "New Game" button to enter the control room.
 */
public class MainMenuScreen extends ScreenAdapter {

    private static final Color BG_COLOR = new Color(0x0A / 255f, 0x0E / 255f, 0x1F / 255f, 1f);
    private static final Color BTN_COLOR = new Color(0.15f, 0.3f, 0.6f, 1f);
    private static final Color BTN_HOVER = new Color(0.2f, 0.4f, 0.8f, 1f);

    private final LhcSimGame game;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont titleFont;
    private BitmapFont infoFont;
    private BitmapFont buttonFont;
    private GlyphLayout layout;

    // Button bounds (set during render)
    private float btnX, btnY, btnW, btnH;

    public MainMenuScreen(LhcSimGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        layout = new GlyphLayout();

        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(3.0f);

        infoFont = new BitmapFont();
        infoFont.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));
        infoFont.getData().setScale(1.5f);

        buttonFont = new BitmapFont();
        buttonFont.setColor(Color.WHITE);
        buttonFont.getData().setScale(2.0f);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    startGame();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float y = Gdx.graphics.getHeight() - screenY;
                if (screenX >= btnX && screenX <= btnX + btnW && y >= btnY && y <= btnY + btnH) {
                    startGame();
                    return true;
                }
                return false;
            }
        });
    }

    private void startGame() {
        SimulationManager sim = new SimulationManager(
                game.getEventBus(), game.getRandomService(),
                game.getTimeManager(), game.getBeamTimeManager(),
                game.getAlertSystem(), game.getParticleDatabase(),
                game.getCrossSectionTable());
        game.setScreen(new ControlRoomScreen(game, sim));
        dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, BG_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Button dimensions
        btnW = 300;
        btnH = 55;
        btnX = (w - btnW) / 2f;
        btnY = h * 0.35f;

        // Check hover
        float mouseX = Gdx.input.getX();
        float mouseY = h - Gdx.input.getY();
        boolean hover = mouseX >= btnX && mouseX <= btnX + btnW
                && mouseY >= btnY && mouseY <= btnY + btnH;

        // Draw button
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hover ? BTN_HOVER : BTN_COLOR);
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.3f, 0.6f, 1.0f, 1f));
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();

        // Draw text
        batch.begin();

        // Title
        String title = "LHC Simulator";
        layout.setText(titleFont, title);
        titleFont.draw(batch, title,
                (w - layout.width) / 2f,
                h * 0.72f);

        // Subtitle
        String subtitle = "Particle Physics Control Room";
        layout.setText(infoFont, subtitle);
        infoFont.draw(batch, subtitle,
                (w - layout.width) / 2f,
                h * 0.72f - 55);

        // Status info
        int particleCount = game.getParticleDatabase() != null
                ? game.getParticleDatabase().size() : 0;
        int processCount = game.getCrossSectionTable() != null
                ? game.getCrossSectionTable().getAllProcesses().size() : 0;

        infoFont.setColor(new Color(0.5f, 0.55f, 0.65f, 1.0f));
        String status = String.format("Loaded %d particles, %d processes", particleCount, processCount);
        layout.setText(infoFont, status);
        infoFont.draw(batch, status,
                (w - layout.width) / 2f,
                h * 0.52f);
        infoFont.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));

        // Button text
        String btnText = "NEW GAME";
        layout.setText(buttonFont, btnText);
        buttonFont.draw(batch, btnText,
                btnX + (btnW - layout.width) / 2f,
                btnY + btnH / 2f + layout.height / 2f);

        // Hint
        infoFont.setColor(new Color(0.4f, 0.4f, 0.5f, 1.0f));
        String hint = "Press ENTER or click to start";
        layout.setText(infoFont, hint);
        infoFont.draw(batch, hint,
                (w - layout.width) / 2f,
                h * 0.22f);
        infoFont.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));

        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (titleFont != null) titleFont.dispose();
        if (infoFont != null) infoFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
    }
}
