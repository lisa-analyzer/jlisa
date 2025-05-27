package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SVCompMini {
    @Test
    public void testAlarmMini() throws IOException {
        String outdir = "tests-outputs/sv-comp-mini/alarm/";
        LiSA lisa = TestHelpers.getLiSA(outdir);
        JavaFrontend frontend = new JavaFrontend();

        List<String> inputs = new ArrayList<>();
        inputs.add("tests/sv-comp-mini/AlarmFunctionalMini.java");
        inputs.add("tests/sv-comp-mini/B.java");
        frontend.parseFromListOfFile(inputs);

        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
            return;
        }
        lisa.run(frontend.getProgram());
    }
}
