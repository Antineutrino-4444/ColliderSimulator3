package com.lhcsim.physics.collision;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Table of physics-process cross-sections as a function of centre-of-mass
 * energy, loaded from a JSON resource.  Provides log-space interpolation
 * between tabulated energies.
 */
public class CrossSectionTable {

    private static final String DEFAULT_RESOURCE = "/data/cross_sections.json";

    private final Map<String, ProcessCrossSection> processes = new LinkedHashMap<>();

    private CrossSectionTable() {
    }

    /**
     * Loads the cross-section table from the given JSON input stream.
     *
     * @param inputStream JSON array of {@link ProcessCrossSection} entries
     * @return populated table
     * @throws IOException on parse failure
     */
    public static CrossSectionTable load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<ProcessCrossSection> list = mapper.readValue(
                inputStream, new TypeReference<List<ProcessCrossSection>>() {});
        CrossSectionTable table = new CrossSectionTable();
        for (ProcessCrossSection p : list) {
            table.processes.put(p.name, p);
        }
        return table;
    }

    /**
     * Loads the default cross-section table from the classpath resource
     * at {@value #DEFAULT_RESOURCE}.
     */
    public static CrossSectionTable loadDefault() throws IOException {
        try (InputStream is = CrossSectionTable.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (is == null) {
                throw new IOException("Default cross-section table not found: " + DEFAULT_RESOURCE);
            }
            return load(is);
        }
    }

    /**
     * Returns the cross-section for the named process at the given √s,
     * using log-space interpolation between the two nearest tabulated energies.
     *
     * @param processName process identifier
     * @param sqrtS       centre-of-mass energy [GeV]
     * @return cross-section [pb], or 0.0 if the process is unknown
     */
    public double getCrossSectionAt(String processName, double sqrtS) {
        ProcessCrossSection proc = processes.get(processName);
        if (proc == null) {
            return 0.0;
        }
        return proc.getCrossSectionAt(sqrtS);
    }

    /** Returns the process descriptor, or {@code null} if not found. */
    public ProcessCrossSection getProcess(String name) {
        return processes.get(name);
    }

    /** Returns an unmodifiable view of all loaded processes. */
    public Map<String, ProcessCrossSection> getAllProcesses() {
        return Collections.unmodifiableMap(processes);
    }

    /**
     * Expected number of events for a given process, √s, and integrated
     * luminosity: N = σ(√s) · L.
     *
     * @param processName process identifier
     * @param sqrtS       centre-of-mass energy [GeV]
     * @param lumiPb      integrated luminosity [pb⁻¹]
     * @return expected event count
     */
    public double expectedEvents(String processName, double sqrtS, double lumiPb) {
        return getCrossSectionAt(processName, sqrtS) * lumiPb;
    }

    // ── Inner class ─────────────────────────────────────────────────

    /**
     * Cross-section data for a single physics process, tabulated at
     * several centre-of-mass energies.
     */
    public static class ProcessCrossSection {

        private String name;
        private String label;
        private String signature;
        private Map<String, Double> crossSections;

        /** Transient TreeMap for interpolation, built lazily. */
        private transient TreeMap<Double, Double> sortedMap;

        /** Default constructor for Jackson. */
        public ProcessCrossSection() {
        }

        public ProcessCrossSection(String name, String label, String signature,
                                   Map<String, Double> crossSections) {
            this.name = name;
            this.label = label;
            this.signature = signature;
            this.crossSections = crossSections;
        }

        /**
         * Returns the cross-section at the requested √s using log–log
         * interpolation between the two nearest tabulated energies.
         */
        public double getCrossSectionAt(double sqrtS) {
            ensureSortedMap();
            if (sortedMap.isEmpty()) {
                return 0.0;
            }
            Map.Entry<Double, Double> floor = sortedMap.floorEntry(sqrtS);
            Map.Entry<Double, Double> ceil = sortedMap.ceilingEntry(sqrtS);

            if (floor == null && ceil == null) {
                return 0.0;
            }
            if (floor == null) {
                return ceil.getValue();
            }
            if (ceil == null) {
                return floor.getValue();
            }
            if (floor.getKey().equals(ceil.getKey())) {
                return floor.getValue();
            }

            // Log–log interpolation
            double logE1 = Math.log(floor.getKey());
            double logE2 = Math.log(ceil.getKey());
            double logS1 = Math.log(floor.getValue());
            double logS2 = Math.log(ceil.getValue());
            double t = (Math.log(sqrtS) - logE1) / (logE2 - logE1);
            return Math.exp(logS1 + t * (logS2 - logS1));
        }

        private void ensureSortedMap() {
            if (sortedMap == null) {
                sortedMap = new TreeMap<>();
                if (crossSections != null) {
                    for (Map.Entry<String, Double> e : crossSections.entrySet()) {
                        sortedMap.put(Double.parseDouble(e.getKey()), e.getValue());
                    }
                }
            }
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public String getSignature() {
            return signature;
        }

        public Map<String, Double> getCrossSections() {
            return crossSections;
        }
    }
}
