package com.lhcsim.physics.collision.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parton distribution function grid for protons.
 * <p>
 * Loads a precomputed 2D table of x·f(x, Q²) over a grid of Bjorken-x
 * and factorization scale Q², indexed by parton PDG ID. Provides bilinear
 * interpolation in (ln x, ln Q²) space.
 * <p>
 * Supported parton PDG IDs: 21 (g), ±1 (d/d̄), ±2 (u/ū), ±3 (s/s̄),
 * ±4 (c/c̄), ±5 (b/b̄).
 * <p>
 * Reference: analytic parametrization inspired by CT18NLO
 * (Hou et al., Phys. Rev. D 103 (2021) 014013).
 */
public class PdfGrid {

    private static final Logger log = LoggerFactory.getLogger(PdfGrid.class);
    private static final String DEFAULT_RESOURCE = "/data/pdf/ct18nlo-grid.json";

    private double[] xGrid;
    private double[] q2Grid;
    private double[] lnxGrid;
    private double[] lnq2Grid;

    /** Map from PDG ID string to [q2_idx][x_idx] values of x·f(x,Q²). */
    private final Map<String, double[][]> partonGrids = new HashMap<>();

    private PdfGrid() {}

    /**
     * Loads the default PDF grid from classpath.
     */
    public static PdfGrid loadDefault() throws IOException {
        try (InputStream is = PdfGrid.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (is == null) {
                throw new IOException("PDF grid not found: " + DEFAULT_RESOURCE);
            }
            return load(is);
        }
    }

    /**
     * Loads a PDF grid from the given input stream.
     */
    public static PdfGrid load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(inputStream);

        PdfGrid grid = new PdfGrid();

        // Read x grid
        JsonNode xNode = root.get("x_grid");
        grid.xGrid = new double[xNode.size()];
        grid.lnxGrid = new double[xNode.size()];
        for (int i = 0; i < xNode.size(); i++) {
            grid.xGrid[i] = xNode.get(i).doubleValue();
            grid.lnxGrid[i] = Math.log(grid.xGrid[i]);
        }

        // Read Q² grid
        JsonNode q2Node = root.get("q2_grid");
        grid.q2Grid = new double[q2Node.size()];
        grid.lnq2Grid = new double[q2Node.size()];
        for (int i = 0; i < q2Node.size(); i++) {
            grid.q2Grid[i] = q2Node.get(i).doubleValue();
            grid.lnq2Grid[i] = Math.log(grid.q2Grid[i]);
        }

        // Read parton grids
        JsonNode partonsNode = root.get("partons");
        var it = partonsNode.fieldNames();
        while (it.hasNext()) {
            String pdgKey = it.next();
            JsonNode partonArray = partonsNode.get(pdgKey);
            int nq2 = partonArray.size();
            int nx = partonArray.get(0).size();
            double[][] values = new double[nq2][nx];
            for (int iq = 0; iq < nq2; iq++) {
                for (int ix = 0; ix < nx; ix++) {
                    values[iq][ix] = partonArray.get(iq).get(ix).doubleValue();
                }
            }
            grid.partonGrids.put(pdgKey, values);
        }

        log.debug("Loaded PDF grid: {} x-points, {} Q²-points, {} partons",
                grid.xGrid.length, grid.q2Grid.length, grid.partonGrids.size());

        return grid;
    }

    /**
     * Returns x·f(x, Q²) for the given parton type, using bilinear
     * interpolation in (ln x, ln Q²) space.
     *
     * @param pdgId parton PDG ID (e.g. 21 for gluon, 2 for u, -2 for ū)
     * @param x     Bjorken-x ∈ (0, 1]
     * @param q2    factorization scale Q² [GeV²]
     * @return x·f(x, Q²), or 0 if the parton is not in the grid
     */
    public double xfx(int pdgId, double x, double q2) {
        double[][] values = partonGrids.get(String.valueOf(pdgId));
        if (values == null) {
            return 0.0;
        }

        double lnx = Math.log(Math.max(x, xGrid[0]));
        double lnq2 = Math.log(Math.max(q2, q2Grid[0]));

        // Find indices for bilinear interpolation in ln space
        int ix = findBin(lnxGrid, lnx);
        int iq = findBin(lnq2Grid, lnq2);

        // Clamp to grid boundaries
        ix = Math.max(0, Math.min(ix, lnxGrid.length - 2));
        iq = Math.max(0, Math.min(iq, lnq2Grid.length - 2));

        // Bilinear interpolation fractions
        double tx = (lnx - lnxGrid[ix]) / (lnxGrid[ix + 1] - lnxGrid[ix]);
        double tq = (lnq2 - lnq2Grid[iq]) / (lnq2Grid[iq + 1] - lnq2Grid[iq]);

        // Clamp fractions to [0, 1] for boundary cases
        tx = Math.max(0, Math.min(1, tx));
        tq = Math.max(0, Math.min(1, tq));

        double f00 = values[iq][ix];
        double f10 = values[iq + 1][ix];
        double f01 = values[iq][ix + 1];
        double f11 = values[iq + 1][ix + 1];

        double result = f00 * (1 - tx) * (1 - tq)
                + f01 * tx * (1 - tq)
                + f10 * (1 - tx) * tq
                + f11 * tx * tq;

        return Math.max(0, result);
    }

    /**
     * Returns f(x, Q²) = xfx / x for the given parton.
     */
    public double fx(int pdgId, double x, double q2) {
        if (x <= 0) return 0;
        return xfx(pdgId, x, q2) / x;
    }

    /**
     * Returns the x grid points.
     */
    public double[] getXGrid() {
        return xGrid.clone();
    }

    /**
     * Returns the Q² grid points.
     */
    public double[] getQ2Grid() {
        return q2Grid.clone();
    }

    /**
     * Binary search for the lower-bound bin index.
     */
    private static int findBin(double[] grid, double value) {
        int lo = 0;
        int hi = grid.length - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) >>> 1;
            if (grid[mid] <= value) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
