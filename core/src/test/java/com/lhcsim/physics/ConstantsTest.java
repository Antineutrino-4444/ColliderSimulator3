package com.lhcsim.physics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link Constants} and {@link Units}.
 * <p>
 * Verifies CODATA 2018 values to 6 significant figures and formatting round-trips.
 */
class ConstantsTest {

    @Test
    void testProtonMassEnergy() {
        // m_p * c^2 in MeV — CODATA 2018 value: 938.272 MeV
        double massEnergyMeV = Constants.m_p * Constants.c * Constants.c
                / (Constants.eV_TO_J * 1e6);
        assertThat(massEnergyMeV).isCloseTo(938.272, within(0.001));
    }

    @Test
    void testProtonMassGeVConstant() {
        assertThat(Constants.PROTON_MASS_GEV).isCloseTo(0.938272, within(0.000001));
    }

    @Test
    void testElectronMassEnergy() {
        // m_e * c^2 in MeV — CODATA 2018 value: 0.510999 MeV
        double massEnergyMeV = Constants.m_e * Constants.c * Constants.c
                / (Constants.eV_TO_J * 1e6);
        assertThat(massEnergyMeV).isCloseTo(0.510999, within(0.000001));
    }

    @Test
    void testSpeedOfLight() {
        assertThat(Constants.c).isEqualTo(299_792_458.0);
    }

    @Test
    void testElementaryCharge() {
        assertThat(Constants.e).isCloseTo(1.602176634e-19, within(1e-28));
    }

    @Test
    void testEVConversionRoundTrip() {
        double energy = 7e12 * Constants.eV_TO_J; // 7 TeV in joules
        double backToEV = energy * Constants.J_TO_eV;
        assertThat(backToEV).isCloseTo(7e12, within(1.0));
    }

    @Test
    void testBarnConversion() {
        assertThat(Constants.BARN_TO_M2).isEqualTo(1e-28);
    }

    @Test
    void testFemtobarnConversion() {
        assertThat(Constants.FB_TO_BARN).isEqualTo(1e-15);
    }

    // ── Units formatting tests ──────────────────────────────────────

    @Test
    void testFormatEnergyTeV() {
        double sevenTeV = 7e12; // eV
        String formatted = Units.formatEnergyEV(sevenTeV);
        assertThat(formatted).isEqualTo("7.00 TeV");
    }

    @Test
    void testFormatEnergyGeV() {
        String formatted = Units.formatEnergyEV(125.25e9);
        assertThat(formatted).isEqualTo("125.25 GeV");
    }

    @Test
    void testFormatEnergyMeV() {
        String formatted = Units.formatEnergyEV(938.272e6);
        assertThat(formatted).isEqualTo("938.27 MeV");
    }

    @Test
    void testFormatCrossSectionPb() {
        String formatted = Units.formatCrossSection(48.6e-12);
        assertThat(formatted).contains("pb");
    }

    @Test
    void testFormatCrossSectionFb() {
        String formatted = Units.formatCrossSection(1.5e-15);
        assertThat(formatted).contains("fb");
    }

    @Test
    void testFormatLuminosity() {
        String formatted = Units.formatLuminosity(2.1e34);
        assertThat(formatted).contains("10^34");
        assertThat(formatted).contains("cm^-2 s^-1");
    }
}
