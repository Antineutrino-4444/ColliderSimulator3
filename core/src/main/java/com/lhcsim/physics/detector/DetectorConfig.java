package com.lhcsim.physics.detector;

/**
 * Parametric detector configuration modelled after the four main LHC experiments.
 * <p>
 * Resolution parameters follow the standard CMS/ATLAS convention:
 * σ(E)/E = A / √E ⊕ B (constant term added in quadrature).
 */
public final class DetectorConfig {

    private final String name;
    private final double solenoidField;   // Tesla
    private final double trackerPtMin;    // GeV – minimum reconstructable pT
    private final double trackerResA;     // tracker σ(pT)/pT stochastic term
    private final double trackerResB;     // tracker σ(pT)/pT constant term
    private final double ecalResA;        // ECAL stochastic term
    private final double ecalResB;        // ECAL constant term
    private final double hcalResA;        // HCAL stochastic term
    private final double hcalResB;        // HCAL constant term
    private final double muonEtaMax;      // muon spectrometer |η| coverage
    private final double trackerEtaMax;   // inner tracker |η| coverage
    private final double length;          // metres
    private final double diameter;        // metres

    private DetectorConfig(Builder b) {
        this.name = b.name;
        this.solenoidField = b.solenoidField;
        this.trackerPtMin = b.trackerPtMin;
        this.trackerResA = b.trackerResA;
        this.trackerResB = b.trackerResB;
        this.ecalResA = b.ecalResA;
        this.ecalResB = b.ecalResB;
        this.hcalResA = b.hcalResA;
        this.hcalResB = b.hcalResB;
        this.muonEtaMax = b.muonEtaMax;
        this.trackerEtaMax = b.trackerEtaMax;
        this.length = b.length;
        this.diameter = b.diameter;
    }

    // ── Static factories ────────────────────────────────────────────

    /** ATLAS-like general-purpose detector. */
    public static DetectorConfig atlas() {
        return new Builder("ATLAS")
                .solenoidField(2.0)
                .trackerPtMin(0.5)
                .trackerRes(0.05, 0.01)
                .ecalRes(0.10, 0.007)
                .hcalRes(0.50, 0.03)
                .muonEtaMax(2.7)
                .trackerEtaMax(2.5)
                .length(46.0)
                .diameter(25.0)
                .build();
    }

    /** CMS-like general-purpose detector with a stronger solenoid. */
    public static DetectorConfig cms() {
        return new Builder("CMS")
                .solenoidField(3.8)
                .trackerPtMin(0.5)
                .trackerRes(0.05, 0.01)
                .ecalRes(0.028, 0.003)
                .hcalRes(1.0, 0.05)
                .muonEtaMax(2.4)
                .trackerEtaMax(2.5)
                .length(21.6)
                .diameter(14.6)
                .build();
    }

    /** LHCb-like forward spectrometer. */
    public static DetectorConfig lhcb() {
        return new Builder("LHCb")
                .solenoidField(1.1)
                .trackerPtMin(0.2)
                .trackerRes(0.05, 0.01)
                .ecalRes(0.10, 0.01)
                .hcalRes(0.70, 0.05)
                .muonEtaMax(5.0)
                .trackerEtaMax(5.0)
                .length(20.0)
                .diameter(6.3)
                .build();
    }

    /** ALICE-like heavy-ion detector. */
    public static DetectorConfig alice() {
        return new Builder("ALICE")
                .solenoidField(0.5)
                .trackerPtMin(0.15)
                .trackerRes(0.05, 0.01)
                .ecalRes(0.11, 0.017)
                .hcalRes(0.80, 0.05)
                .muonEtaMax(4.0)
                .trackerEtaMax(0.9)
                .length(26.0)
                .diameter(16.0)
                .build();
    }

    // ── Builder ─────────────────────────────────────────────────────

    public static final class Builder {
        private final String name;
        private double solenoidField = 2.0;
        private double trackerPtMin = 0.5;
        private double trackerResA = 0.05;
        private double trackerResB = 0.01;
        private double ecalResA = 0.10;
        private double ecalResB = 0.007;
        private double hcalResA = 0.50;
        private double hcalResB = 0.03;
        private double muonEtaMax = 2.7;
        private double trackerEtaMax = 2.5;
        private double length = 46.0;
        private double diameter = 25.0;

        public Builder(String name) {
            this.name = name;
        }

        public Builder solenoidField(double t) { this.solenoidField = t; return this; }
        public Builder trackerPtMin(double gev) { this.trackerPtMin = gev; return this; }
        public Builder trackerRes(double a, double b) { this.trackerResA = a; this.trackerResB = b; return this; }
        public Builder ecalRes(double a, double b) { this.ecalResA = a; this.ecalResB = b; return this; }
        public Builder hcalRes(double a, double b) { this.hcalResA = a; this.hcalResB = b; return this; }
        public Builder muonEtaMax(double eta) { this.muonEtaMax = eta; return this; }
        public Builder trackerEtaMax(double eta) { this.trackerEtaMax = eta; return this; }
        public Builder length(double m) { this.length = m; return this; }
        public Builder diameter(double m) { this.diameter = m; return this; }

        public DetectorConfig build() { return new DetectorConfig(this); }
    }

    // ── Getters ─────────────────────────────────────────────────────

    public String getName()          { return name; }
    public double getSolenoidField() { return solenoidField; }
    public double getTrackerPtMin()  { return trackerPtMin; }
    public double getTrackerResA()   { return trackerResA; }
    public double getTrackerResB()   { return trackerResB; }
    public double getEcalResA()      { return ecalResA; }
    public double getEcalResB()      { return ecalResB; }
    public double getHcalResA()      { return hcalResA; }
    public double getHcalResB()      { return hcalResB; }
    public double getMuonEtaMax()    { return muonEtaMax; }
    public double getTrackerEtaMax() { return trackerEtaMax; }
    public double getLength()        { return length; }
    public double getDiameter()      { return diameter; }

    @Override
    public String toString() {
        return name + " [B=" + solenoidField + " T, tracker |η|<" + trackerEtaMax + "]";
    }
}
