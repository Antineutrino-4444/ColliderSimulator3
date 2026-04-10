package com.lhcsim.physics;

/**
 * Display-layer unit formatting for physics quantities.
 * <p>
 * Internal representation is always SI; these methods produce
 * human-readable strings with appropriate units and engineering
 * notation.
 */
public final class Units {

    private Units() {}

    /**
     * Formats an energy given in joules as a human-readable string
     * with the most appropriate unit (eV, keV, MeV, GeV, TeV).
     *
     * @param joules energy in joules
     * @return formatted string, e.g. "7.00 TeV"
     */
    public static String formatEnergy(double joules) {
        double eV = joules * Constants.J_TO_eV;
        return formatEnergyEV(eV);
    }

    /**
     * Formats an energy given in eV as a human-readable string
     * with the most appropriate unit (eV, keV, MeV, GeV, TeV).
     *
     * @param eV energy in electron-volts
     * @return formatted string, e.g. "7.00 TeV"
     */
    public static String formatEnergyEV(double eV) {
        double absEV = Math.abs(eV);
        if (absEV >= 1e12) {
            return String.format("%.2f TeV", eV / 1e12);
        } else if (absEV >= 1e9) {
            return String.format("%.2f GeV", eV / 1e9);
        } else if (absEV >= 1e6) {
            return String.format("%.2f MeV", eV / 1e6);
        } else if (absEV >= 1e3) {
            return String.format("%.2f keV", eV / 1e3);
        } else {
            return String.format("%.2f eV", eV);
        }
    }

    /**
     * Formats a luminosity in CGS units (cm^-2 s^-1) as a readable string.
     * <p>
     * Example: 2.1e34 becomes "2.10 x 10^34 cm^-2 s^-1".
     *
     * @param cgs luminosity in cm^-2 s^-1
     * @return formatted string
     */
    public static String formatLuminosity(double cgs) {
        if (cgs == 0.0) {
            return "0 cm^-2 s^-1";
        }
        int exponent = (int) Math.floor(Math.log10(Math.abs(cgs)));
        double mantissa = cgs / Math.pow(10, exponent);
        return String.format("%.2f x 10^%d cm^-2 s^-1", mantissa, exponent);
    }

    /**
     * Formats a cross-section in barns as a human-readable string
     * with the most appropriate unit (b, mb, ub, nb, pb, fb).
     *
     * @param barns cross-section in barns
     * @return formatted string, e.g. "48.6 pb"
     */
    public static String formatCrossSection(double barns) {
        double abs = Math.abs(barns);
        if (abs >= 1.0) {
            return String.format("%.3g b", barns);
        } else if (abs >= 1e-3) {
            return String.format("%.3g mb", barns * 1e3);
        } else if (abs >= 1e-6) {
            return String.format("%.3g ub", barns * 1e6);
        } else if (abs >= 1e-9) {
            return String.format("%.3g nb", barns * 1e9);
        } else if (abs >= 1e-12) {
            return String.format("%.3g pb", barns * 1e12);
        } else {
            return String.format("%.3g fb", barns * 1e15);
        }
    }

    /**
     * Formats a magnetic field in Tesla.
     *
     * @param tesla field strength in Tesla
     * @return formatted string, e.g. "8.33 T"
     */
    public static String formatMagneticField(double tesla) {
        if (Math.abs(tesla) < 1e-3) {
            return String.format("%.2f mT", tesla * 1e3);
        }
        return String.format("%.2f T", tesla);
    }

    /**
     * Formats a length in metres with appropriate sub-units.
     *
     * @param metres length in metres
     * @return formatted string
     */
    public static String formatLength(double metres) {
        double abs = Math.abs(metres);
        if (abs >= 1e3) {
            return String.format("%.2f km", metres / 1e3);
        } else if (abs >= 1.0) {
            return String.format("%.2f m", metres);
        } else if (abs >= 1e-3) {
            return String.format("%.2f mm", metres * 1e3);
        } else if (abs >= 1e-6) {
            return String.format("%.2f um", metres * 1e6);
        } else {
            return String.format("%.2f nm", metres * 1e9);
        }
    }
}
