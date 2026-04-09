package com.lhcsim.game.analysis;

/**
 * Fixed-bin-width histogram used for in-game analysis displays
 * (e.g. invariant-mass distributions, pT spectra).
 */
public class HistogramAccumulator {

    private final String name;
    private final String xAxisLabel;
    private final String yAxisLabel;
    private final double xMin;
    private final double xMax;
    private final int numBins;
    private final double binWidth;

    private final double[] binContents;
    private final double[] binErrors;   // sum of weights² per bin
    private int totalEntries;

    public HistogramAccumulator(String name, String xAxisLabel, String yAxisLabel,
                                double xMin, double xMax, int numBins) {
        if (numBins <= 0) {
            throw new IllegalArgumentException("numBins must be > 0");
        }
        if (xMax <= xMin) {
            throw new IllegalArgumentException("xMax must be > xMin");
        }
        this.name = name;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
        this.xMin = xMin;
        this.xMax = xMax;
        this.numBins = numBins;
        this.binWidth = (xMax - xMin) / numBins;
        this.binContents = new double[numBins];
        this.binErrors = new double[numBins];
    }

    /** Fills the histogram with unit weight. */
    public void fill(double value) {
        fill(value, 1.0);
    }

    /** Fills the histogram with an arbitrary weight. */
    public void fill(double value, double weight) {
        int bin = toBin(value);
        if (bin < 0 || bin >= numBins) {
            return; // overflow / underflow – ignored
        }
        binContents[bin] += weight;
        binErrors[bin] += weight * weight;
        totalEntries++;
    }

    /** Returns the content (sum of weights) for the given bin index. */
    public double getBinContent(int bin) {
        checkBin(bin);
        return binContents[bin];
    }

    /** Returns √(Σw²) for the given bin (statistical error). */
    public double getBinError(int bin) {
        checkBin(bin);
        return Math.sqrt(binErrors[bin]);
    }

    /** Returns the centre x-value of the given bin. */
    public double getBinCenter(int bin) {
        checkBin(bin);
        return xMin + (bin + 0.5) * binWidth;
    }

    /**
     * Simple signal significance estimator: (s − b) / √b.
     *
     * @param signalLo          lower edge of the signal window
     * @param signalHi          upper edge of the signal window
     * @param backgroundEstimate expected background in that window
     * @return significance, or 0 if background ≤ 0
     */
    public double computeSignificance(double signalLo, double signalHi,
                                      double backgroundEstimate) {
        if (backgroundEstimate <= 0) {
            return 0;
        }
        double signal = 0;
        for (int i = 0; i < numBins; i++) {
            double center = getBinCenter(i);
            if (center >= signalLo && center <= signalHi) {
                signal += binContents[i];
            }
        }
        return (signal - backgroundEstimate) / Math.sqrt(backgroundEstimate);
    }

    /** Resets all bin contents and errors to zero. */
    public void reset() {
        java.util.Arrays.fill(binContents, 0);
        java.util.Arrays.fill(binErrors, 0);
        totalEntries = 0;
    }

    // ── Getters ─────────────────────────────────────────────────────

    public String getName()       { return name; }
    public String getXAxisLabel() { return xAxisLabel; }
    public String getYAxisLabel() { return yAxisLabel; }
    public double getXMin()       { return xMin; }
    public double getXMax()       { return xMax; }
    public int getNumBins()       { return numBins; }
    public double getBinWidth()   { return binWidth; }
    public int getTotalEntries()  { return totalEntries; }

    // ── Internal ────────────────────────────────────────────────────

    private int toBin(double value) {
        return (int) ((value - xMin) / binWidth);
    }

    private void checkBin(int bin) {
        if (bin < 0 || bin >= numBins) {
            throw new IndexOutOfBoundsException("Bin " + bin + " out of range [0, " + numBins + ")");
        }
    }
}
