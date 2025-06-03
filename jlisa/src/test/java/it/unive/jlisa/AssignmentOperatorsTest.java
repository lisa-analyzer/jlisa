package it.unive.jlisa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;

public class AssignmentOperatorsTest {
	
    @Test
    public void testAsgOperators() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/asg-operators/Main.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/asg-operators");
        lisa.run(frontend.getProgram());

    }
}
