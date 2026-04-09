package com.lhcsim.physics.accelerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads accelerator lattice definitions from JSON files.
 * <p>
 * Each JSON file describes a ring with a name, circumference, reference
 * energy, and an ordered list of element entries. Each entry has a type
 * (QUAD, DIPOLE, DRIFT, SEXTUPOLE, RFCAVITY, BPM, MARKER) and
 * type-specific parameters (length, gradient, field, voltage, etc.).
 */
public final class LatticeLoader {

    private static final Logger log = LoggerFactory.getLogger(LatticeLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LatticeLoader() {}

    /**
     * Loads a lattice from a classpath resource.
     *
     * @param resourcePath path to the JSON resource (e.g. "/data/lattices/ps.json")
     * @return the loaded lattice
     * @throws IOException if the resource cannot be found or parsed
     */
    public static Lattice loadResource(String resourcePath) throws IOException {
        try (InputStream is = LatticeLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Lattice resource not found: " + resourcePath);
            }
            return load(is);
        }
    }

    /**
     * Loads a lattice from an input stream containing JSON.
     *
     * @param inputStream the JSON input
     * @return the loaded lattice
     * @throws IOException if the stream cannot be read or parsed
     */
    public static Lattice load(InputStream inputStream) throws IOException {
        JsonNode root = MAPPER.readTree(inputStream);

        String name = root.has("name") ? root.get("name").asText() : "unknown";
        double circumference = root.has("circumference") ? root.get("circumference").asDouble() : 0;

        Lattice lattice = new Lattice(name, circumference);

        JsonNode elements = root.get("elements");
        if (elements == null || !elements.isArray()) {
            throw new IOException("Lattice JSON must have an 'elements' array");
        }

        for (JsonNode el : elements) {
            AcceleratorElement element = parseElement(el);
            lattice.addElement(element);
        }

        log.info("Loaded lattice '{}': {} elements, total length {} m",
                name, lattice.getElements().size(), String.format("%.2f", lattice.totalLength()));

        return lattice;
    }

    private static AcceleratorElement parseElement(JsonNode node) {
        String type = node.get("type").asText().toUpperCase();
        String elName = node.has("name") ? node.get("name").asText() : type;
        double length = node.has("length") ? node.get("length").asDouble() : 0;

        return switch (type) {
            case "DRIFT" -> new Drift(elName, length);
            case "QUAD", "QUADRUPOLE" -> {
                double gradient = node.get("gradient").asDouble();
                yield new Quadrupole(elName, length, gradient);
            }
            case "DIPOLE" -> {
                double field = node.get("field").asDouble();
                yield new Dipole(elName, length, field);
            }
            case "SEXTUPOLE", "SEXT" -> {
                double strength = node.has("strength") ? node.get("strength").asDouble() : 0;
                yield new Sextupole(elName, length, strength);
            }
            case "RFCAVITY", "RF" -> {
                double voltage = node.has("voltage") ? node.get("voltage").asDouble() : 0;
                double frequency = node.has("frequency") ? node.get("frequency").asDouble() : 0;
                int harmonicNumber = node.has("harmonicNumber") ? node.get("harmonicNumber").asInt() : 1;
                double syncPhase = node.has("synchronousPhase") ? node.get("synchronousPhase").asDouble() : 0;
                double circ = node.has("circumference") ? node.get("circumference").asDouble() : 0;
                yield new RFCavity(elName, length, voltage, frequency, harmonicNumber, syncPhase, circ);
            }
            case "BPM" -> new BeamPositionMonitor(elName);
            case "MARKER" -> new Marker(elName);
            default -> {
                log.warn("Unknown element type '{}', treating as drift", type);
                yield new Drift(elName, length);
            }
        };
    }
}
