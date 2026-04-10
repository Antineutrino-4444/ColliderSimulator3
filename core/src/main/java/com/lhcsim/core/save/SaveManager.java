package com.lhcsim.core.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lhcsim.core.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Handles JSON serialisation / deserialisation of {@link GameState}
 * to per-profile save files.
 */
public final class SaveManager {

    private static final Logger log = LoggerFactory.getLogger(SaveManager.class);
    private static final int MAX_PROFILES = 3;

    private final ObjectMapper mapper;
    private final File saveDir;

    public SaveManager(File saveDir) {
        this.saveDir = saveDir;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Persist the given game state to disk for the specified profile.
     */
    public void save(GameState state, int profileIndex) throws IOException {
        validateProfile(profileIndex);
        state.setProfileIndex(profileIndex);
        File file = profileFile(profileIndex);
        saveDir.mkdirs();
        mapper.writeValue(file, state);
        log.info("Game saved to {}", file.getAbsolutePath());
    }

    /**
     * Load and migrate a saved game state from disk.
     *
     * @return the loaded (and migrated) state, or {@code null} if no save exists
     */
    public GameState load(int profileIndex) throws IOException {
        validateProfile(profileIndex);
        File file = profileFile(profileIndex);
        if (!file.exists()) {
            return null;
        }
        GameState state = mapper.readValue(file, GameState.class);
        Migrations.migrate(state);
        log.info("Game loaded from {} (version {})", file.getAbsolutePath(), state.getSaveVersion());
        return state;
    }

    public boolean hasSave(int profileIndex) {
        validateProfile(profileIndex);
        return profileFile(profileIndex).exists();
    }

    public boolean deleteSave(int profileIndex) {
        validateProfile(profileIndex);
        File file = profileFile(profileIndex);
        boolean deleted = file.delete();
        if (deleted) {
            log.info("Deleted save profile {}", profileIndex);
        }
        return deleted;
    }

    // ── Helpers ────────────────────────────────────────────────────

    private File profileFile(int profileIndex) {
        return new File(saveDir, "profile_" + profileIndex + ".json");
    }

    private static void validateProfile(int profileIndex) {
        if (profileIndex < 0 || profileIndex >= MAX_PROFILES) {
            throw new IllegalArgumentException(
                    "Profile index must be 0.." + (MAX_PROFILES - 1) + ", got " + profileIndex);
        }
    }
}
