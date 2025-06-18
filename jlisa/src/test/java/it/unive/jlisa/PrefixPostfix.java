package it.unive.jlisa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;

public class PrefixPostfix {

    @Test
    public void testPrefixPostfix() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        List<String> inputs = new ArrayList<>();
        inputs.add("tests/prefix-postfix/Main.java");
        frontend.parseFromListOfFile(inputs);
        LiSA lisa = TestHelpers.getLiSA("tests-output/prefix-postfix");
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
