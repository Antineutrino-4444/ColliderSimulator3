package com.lhcsim.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads {@link AppConfig} from {@code ~/.lhcsim/config.json}.
 * If the file does not exist, copies the built-in default from
 * {@code resources/default-config.json}.
 */
public final class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.lhcsim";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    private static final String DEFAULT_RESOURCE = "/default-config.json";

    private ConfigLoader() { /* utility class */ }

    /**
     * Load configuration from disk, creating the file from defaults if missing.
     */
    public static AppConfig load() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            log.info("No config found at {}; creating from defaults", CONFIG_FILE);
            return createDefault(mapper, configFile);
        }

        try {
            AppConfig config = mapper.readValue(configFile, AppConfig.class);
            log.info("Loaded config from {}", CONFIG_FILE);
            return config;
        } catch (IOException e) {
            log.warn("Failed to read config; using defaults", e);
            return new AppConfig();
        }
    }

    private static AppConfig createDefault(ObjectMapper mapper, File configFile) {
        AppConfig defaults = new AppConfig();

        // Try loading from classpath resource
        try (InputStream is = ConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (is != null) {
                defaults = mapper.readValue(is, AppConfig.class);
            }
        } catch (IOException e) {
            log.warn("Could not parse default-config.json resource", e);
        }

        // Write to disk
        try {
            configFile.getParentFile().mkdirs();
            mapper.writeValue(configFile, defaults);
            log.info("Wrote default config to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not persist config to disk", e);
        }

        return defaults;
    }
}
