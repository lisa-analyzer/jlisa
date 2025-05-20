package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoopTests {

    @Test
    public void testWhile1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("inputs/loops/while/while-1.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("outputs/loops/while/while-1");
        lisa.run(frontend.getProgram());
    }

    @Test
    public void testDoWhile1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("inputs/loops/do-while/do-while-1.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("outputs/loops/do-while/do-while-1");
        lisa.run(frontend.getProgram());
    }
}
