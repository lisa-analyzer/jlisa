package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LiteralsTest {
    @Test
    public void testByte1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/literals/ByteTest.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("tests-outputs/literals/byte-1");
        lisa.run(frontend.getProgram());
    }

    @Test
    public void testChar1() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/literals/CharTest.java");
        frontend.parseFromListOfFile(inputs);

        LiSA lisa = TestHelpers.getLiSA("tests-outputs/literals/char-1");
        lisa.run(frontend.getProgram());
    }
}
