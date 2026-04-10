package com.lhcsim.core.save;

import com.lhcsim.core.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies incremental migrations to bring a {@link GameState} from an older
 * save version up to the current one.
 */
public final class Migrations {

    private static final Logger log = LoggerFactory.getLogger(Migrations.class);

    /** The latest save-format version understood by this build. */
    public static final int CURRENT_SAVE_VERSION = 1;

    private Migrations() { /* utility class */ }

    /**
     * Migrate the given state in-place until it reaches
     * {@link #CURRENT_SAVE_VERSION}.
     */
    public static void migrate(GameState state) {
        while (state.getSaveVersion() < CURRENT_SAVE_VERSION) {
            int from = state.getSaveVersion();
            applyMigration(state, from + 1);
            state.setSaveVersion(from + 1);
            log.info("Migrated save from version {} to {}", from, from + 1);
        }
    }

    /**
     * Apply a single migration step to reach {@code targetVersion}.
     * Add new cases here as the save format evolves.
     */
    private static void applyMigration(GameState state, int targetVersion) {
        switch (targetVersion) {
            // Example: case 2 → add default values for fields introduced in v2
            default -> log.warn("No migration logic defined for version {}", targetVersion);
        }
    }
}
