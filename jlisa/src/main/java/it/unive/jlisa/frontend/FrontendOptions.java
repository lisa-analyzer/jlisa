package it.unive.jlisa.frontend;

public class FrontendOptions {
    
    private int loopUnrollingFactor = 2;
    
    /**
     * Enumeration defining strategies for handling parsing exceptions.
     *
     * <ul>
     * <li>{@code FAIL} - Immediately throw a RuntimeException when a parsing exception occurs</li>
     * <li>{@code COLLECT} - Collect parsing exceptions for later analysis without stopping the parsing process</li>
     * </ul>
     */
    public enum EXCEPTION_HANDLING_STRATEGY {
        /** Fail immediately when a parsing exception occurs */
        FAIL,
        /** Collect exceptions and continue parsing */
        COLLECT
    }
    
    /** The strategy used for handling parsing exceptions */
    private EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy = EXCEPTION_HANDLING_STRATEGY.FAIL;

    private FrontendOptions(final Builder b) {
        this.loopUnrollingFactor = b.getLoopUnrollingFactor();
        this.exceptionHandlingStrategy = b.exceptionHandlingStrategy;
    }

    public static class Builder {
        private int loopUnrollingFactor = 0;
        private EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy = EXCEPTION_HANDLING_STRATEGY.FAIL;
        public Builder() {}

        public Builder loopUnrollingFactor(final int loopUnrollingFactor) {
            this.loopUnrollingFactor = loopUnrollingFactor;
        }

        public EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy(final EXCEPTION_HANDLING_STRATEGY exceptionHandlingStrategy) {
            this.exceptionHandlingStrategy = exceptionHandlingStrategy;
        }

        public FrontendOptions build() {
            return new FrontendOptions(this);
        }
    }

    public int getLoopUnrollingFactor() {
        return loopUnrollingFactor;
    }
}