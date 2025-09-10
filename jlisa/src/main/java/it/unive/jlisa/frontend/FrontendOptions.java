package it.unive.jlisa.frontend;

public class FrontendOptions {
    private int loopUnrollingFactor = 2;
    
    private FrontendOptions(Builder b) {
        this.loopUnrollingFactor = b.getLoopUnrollingFactor();
    }

    public static class Builder {
        private int loopUnrollingFactor = 2;
    
        public Builder() {}

        public Builder loopUnrollingFactor(final int loopUnrollingFactor) {
            this.loopUnrollingFactor = loopUnrollingFactor;
        }

        public FrontendOptions build() {
            return new FrontendOptions(this);
        }
    }

    public int getLoopUnrollingFactor() {
        return loopUnrollingFactor;
    }
}