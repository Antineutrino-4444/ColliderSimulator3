package com.lhcsim.core.save;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-serialisable save file POJO with versioning.
 * <p>
 * Each section blob is a per-system state map keyed by system name.
 * See §14 rule 14.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveFile {

    private int saveVersion = 1;
    private String profileName;
    private long masterSeed;
    private Instant createdAt;
    private Instant lastSavedAt;
    private Map<String, Object> sections = new LinkedHashMap<>();

    public SaveFile() {}

    // ── Getters & Setters ──────────────────────────────────────────

    public int getSaveVersion() { return saveVersion; }
    public void setSaveVersion(int saveVersion) { this.saveVersion = saveVersion; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public long getMasterSeed() { return masterSeed; }
    public void setMasterSeed(long masterSeed) { this.masterSeed = masterSeed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastSavedAt() { return lastSavedAt; }
    public void setLastSavedAt(Instant lastSavedAt) { this.lastSavedAt = lastSavedAt; }

    public Map<String, Object> getSections() { return sections; }
    public void setSections(Map<String, Object> sections) { this.sections = sections; }
}
