package com.lhcsim.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialisable POJO that captures the entire saveable state of the game.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState {

    private int saveVersion = 1;
    private long masterSeed;
    private double inGameHours;
    private String currentEra;
    private String activeMachine;
    private String difficulty;

    private double integratedLuminosity;
    private double beamTimeRemaining;
    private List<String> unlockedComponents = new ArrayList<>();
    private List<String> discoveries = new ArrayList<>();

    private Map<String, Double> beamParameters = new HashMap<>();
    private int profileIndex;
    private int autoFillCompletions;

    // ── Getters & Setters ──────────────────────────────────────────

    public int getSaveVersion() {
        return saveVersion;
    }

    public void setSaveVersion(int saveVersion) {
        this.saveVersion = saveVersion;
    }

    public long getMasterSeed() {
        return masterSeed;
    }

    public void setMasterSeed(long masterSeed) {
        this.masterSeed = masterSeed;
    }

    public double getInGameHours() {
        return inGameHours;
    }

    public void setInGameHours(double inGameHours) {
        this.inGameHours = inGameHours;
    }

    public String getCurrentEra() {
        return currentEra;
    }

    public void setCurrentEra(String currentEra) {
        this.currentEra = currentEra;
    }

    public String getActiveMachine() {
        return activeMachine;
    }

    public void setActiveMachine(String activeMachine) {
        this.activeMachine = activeMachine;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public double getIntegratedLuminosity() {
        return integratedLuminosity;
    }

    public void setIntegratedLuminosity(double integratedLuminosity) {
        this.integratedLuminosity = integratedLuminosity;
    }

    public double getBeamTimeRemaining() {
        return beamTimeRemaining;
    }

    public void setBeamTimeRemaining(double beamTimeRemaining) {
        this.beamTimeRemaining = beamTimeRemaining;
    }

    public List<String> getUnlockedComponents() {
        return unlockedComponents;
    }

    public void setUnlockedComponents(List<String> unlockedComponents) {
        this.unlockedComponents = unlockedComponents;
    }

    public List<String> getDiscoveries() {
        return discoveries;
    }

    public void setDiscoveries(List<String> discoveries) {
        this.discoveries = discoveries;
    }

    public Map<String, Double> getBeamParameters() {
        return beamParameters;
    }

    public void setBeamParameters(Map<String, Double> beamParameters) {
        this.beamParameters = beamParameters;
    }

    public int getProfileIndex() {
        return profileIndex;
    }

    public void setProfileIndex(int profileIndex) {
        this.profileIndex = profileIndex;
    }

    public int getAutoFillCompletions() {
        return autoFillCompletions;
    }

    public void setAutoFillCompletions(int autoFillCompletions) {
        this.autoFillCompletions = autoFillCompletions;
    }
}
