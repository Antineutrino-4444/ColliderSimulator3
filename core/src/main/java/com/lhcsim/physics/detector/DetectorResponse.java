package com.lhcsim.physics.detector;

import java.util.List;

public record DetectorResponse(List<RecoObject> objects, List<RawHit> rawHits, List<List<double[]>> helixPolylines) {}
