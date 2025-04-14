package it.unive.jlisa.frontend;

import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.jlisa.program.language.JavaLanguageFeatures;

public class JavaFrontend {

    public static Program parseFromString(String fileName) {
        JavaLanguageFeatures features = new JavaLanguageFeatures();
        JavaTypeSystem typeSystem = new JavaTypeSystem();
        return new Program(features, typeSystem);
    }
}
