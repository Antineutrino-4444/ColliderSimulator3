package com.lhcsim.physics.detector;

import com.lhcsim.physics.collision.GeneratedEvent;

public interface Detector {
    String name();
    DetectorResponse simulate(GeneratedEvent event);
}
