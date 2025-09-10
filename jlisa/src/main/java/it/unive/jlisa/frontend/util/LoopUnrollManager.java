package it.unive.jlisa.frontend.util;

class LoopUnrollManager {
    private final int loopUnrollFactor = 0;
    private final SyntheticCodeLocationManager locationManager;

    public LoopUnrollManager(final int loopUnrollFactor, final SyntheticCodeLocationManager locationManager) {
        this.loopUnrollingFactor = loopUnrollFactor;
        this.locationManager = locationManager;
    }

    private void getLoopCondition() {}
    
    private void createConditionForUnrolledIteration() {}
    public void unroll() {}
    public boolean canBeUnrolled() {}
}