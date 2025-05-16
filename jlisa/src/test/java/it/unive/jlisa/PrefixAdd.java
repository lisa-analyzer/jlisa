package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrefixAdd {

    @Test
    public void testPrefixAdd() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/prefix/add/Main.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/prefix/add");
        lisa.run(frontend.getProgram());
    }

    @Test
    public void testReturn() throws IOException {
        LiSAConfiguration conf = new LiSAConfiguration();
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/return-stmt/return/Main.java");
        inputs.add("tests/return-stmt/return/A.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/return-stmt/return");
        lisa.run(frontend.getProgram());

    }
}
