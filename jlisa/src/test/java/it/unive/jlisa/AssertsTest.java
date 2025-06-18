package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssertsTest {

    @Test
    public void testAsserts() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/asserts/asserts.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/assert-stmt/asserts");
        lisa.run(frontend.getProgram());
    }


}
