package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Constructors {

    @Test
    public void testThisConstructor() throws IOException {
        String outdir = "tests-outputs/this-constructor/";
        LiSA lisa = TestHelpers.getLiSA(outdir);
        JavaFrontend frontend = new JavaFrontend();

        List<String> inputs = new ArrayList<>();
        inputs.add("tests/constructors/this-constructors/Main.java");
        inputs.add("tests/constructors/this-constructors/B.java");
        inputs.add("tests/constructors/this-constructors/C.java");
        frontend.parseFromListOfFile(inputs);

        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
            return;
        }
        lisa.run(frontend.getProgram());
    }

    @Test
    public void testDefaultConstructor1() throws IOException {
        String outdir = "tests-outputs/default-constructor/1/";
        LiSA lisa = TestHelpers.getLiSA(outdir);
        JavaFrontend frontend = new JavaFrontend();

        List<String> inputs = new ArrayList<>();
        inputs.add("tests/constructors/default-constructor/Main.java");
        inputs.add("tests/constructors/default-constructor/B.java");
        frontend.parseFromListOfFile(inputs);

        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
            return;
        }
        lisa.run(frontend.getProgram());
    }
}
