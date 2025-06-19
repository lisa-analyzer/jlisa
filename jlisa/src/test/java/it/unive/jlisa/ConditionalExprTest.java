package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConditionalExprTest {

    @Test
    public void testAsserts() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/conditional-expr/conditionalExpr.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/conditional-expr/conditionalexpr");
        lisa.run(frontend.getProgram());
    }


}
