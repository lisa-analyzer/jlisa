package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parenthesized {
    @Test
    public void parenthesizedTest1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/parenthesized-expr/Main.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("tests-outputs/parenthesized-expr/parenthesized-expr-1");
        lisa.run(frontend.getProgram());
    }
}
