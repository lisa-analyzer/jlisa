package it.unive.jlisa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;

public class ExtendsTest {
    @Test
    void basicExtends() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        String outdir = "outputs/test/";
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/prop8/Main.java");
        //
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/DW.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/Divs32.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/B.java");
        List<String> inputs = new ArrayList<>();
        //inputs.add("inputs/Test.java");
        inputs.add("tests/extends/basic-extends/Main.java");
        inputs.add("tests/extends/basic-extends/A.java");
        inputs.add("tests/extends/basic-extends/B.java");
        frontend.parseFromListOfFile(inputs);
        if (!frontend.getParserContext().getExceptions().isEmpty()) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
            return;
        }

        //frontend.parseFromFile("inputs/module-info.java");
        //frontend.parseFromFile("inputs/Test.java");
        //Program p = JavaFrontend.parseFromFile("src/main/java/it/unive/jlisa/frontend/JavaFrontend.java");
        Program p = frontend.getProgram();
        assert p.getUnit("B") instanceof ClassUnit : "B must be a ClassUnit";
        assert p.getUnit("A") instanceof ClassUnit : "A must be a ClassUnit";
        assert ((ClassUnit) p.getUnit("B")).getImmediateAncestors().contains(((ClassUnit) p.getUnit("A"))) : "B ancestor not set.";

    }
}