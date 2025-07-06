package it.unive.jlisa.program.location;

import it.unive.lisa.program.SourceCodeLocation;

public class JavaRuntimeLocation extends SourceCodeLocation{
    public static JavaRuntimeLocation INSTANCE = new JavaRuntimeLocation();
    private JavaRuntimeLocation() {
        super("java-runtime", 0, 0);
    }
    
}
