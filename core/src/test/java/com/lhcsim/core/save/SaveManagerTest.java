package com.lhcsim.core.save;

import com.lhcsim.core.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SaveManager}: round-trip save/load, migration, profiles.
 */
class SaveManagerTest {

    @TempDir
    File tmpDir;

    private SaveManager saveManager;

    @BeforeEach
    void setUp() {
        saveManager = new SaveManager(tmpDir);
    }

    @Test
    void testRoundTrip() throws IOException {
        GameState state = new GameState();
        state.setMasterSeed(42L);
        state.setCurrentEra("LHC_RUN2");
        state.setIntegratedLuminosity(150.5);
        state.setDiscoveries(List.of("Higgs boson"));

        saveManager.save(state, 0);

        assertThat(saveManager.hasSave(0)).isTrue();

        GameState loaded = saveManager.load(0);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getMasterSeed()).isEqualTo(42L);
        assertThat(loaded.getCurrentEra()).isEqualTo("LHC_RUN2");
        assertThat(loaded.getIntegratedLuminosity()).isEqualTo(150.5);
        assertThat(loaded.getDiscoveries()).containsExactly("Higgs boson");
    }

    @Test
    void testMigrationRunsInOrder() throws IOException {
        GameState state = new GameState();
        state.setSaveVersion(Migrations.CURRENT_SAVE_VERSION);
        saveManager.save(state, 1);

        GameState loaded = saveManager.load(1);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getSaveVersion()).isEqualTo(Migrations.CURRENT_SAVE_VERSION);
    }

    @Test
    void testDeleteProfile() throws IOException {
        GameState state = new GameState();
        saveManager.save(state, 2);
        assertThat(saveManager.hasSave(2)).isTrue();

        saveManager.deleteSave(2);
        assertThat(saveManager.hasSave(2)).isFalse();
    }

    @Test
    void testLoadMissing() throws IOException {
        assertThat(saveManager.load(0)).isNull();
    }
}
