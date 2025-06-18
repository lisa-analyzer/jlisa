package it.unive.jlisa.analysis;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;

public class ExtendsTest {
	
    @Test
    void basicExtends() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/prop8/Main.java");
        //
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/DW.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/Divs32.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/B.java");
        List<String> inputs = new ArrayList<>();
        //inputs.add("inputs/Test.java");
        inputs.add("tests/extends/basic-extends/Main.java");
        inputs.add("tests/extends/basic-extends/A.java");
        inputs.add("tests/extends/basic-extends/B.java");
        frontend.parseFromListOfFile(inputs);

        //frontend.parseFromFile("inputs/module-info.java");
        //frontend.parseFromFile("inputs/Test.java");
        //Program p = JavaFrontend.parseFromFile("src/main/java/it/unive/jlisa/frontend/JavaFrontend.java");
        Program p = frontend.getProgram();
        
        assertTrue(p.getUnit("B") instanceof ClassUnit);
        assertTrue(p.getUnit("A") instanceof ClassUnit);
        assertTrue(((ClassUnit) p.getUnit("B")).getImmediateAncestors().contains(((ClassUnit) p.getUnit("A"))));

    }
}