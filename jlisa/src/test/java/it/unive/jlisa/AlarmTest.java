package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlarmTest {
    @Test
    void simpleClassTest() throws IOException {
        JavaFrontend frontend = new JavaFrontend();
        String outdir = "outputs/test/";
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/prop8/Main.java");
        //
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/DW.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/Divs32.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/B.java");
        List<String> inputs = new ArrayList<>();
        inputs.add("inputs/java-ranger-regression/alarm/impl/B.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/AlarmOutputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/ConfigOutputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/DeviceConfigurationInputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/DeviceSensorInputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/Divs32.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/DrugDatabaseInputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/DW.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/InfusionManagerOutputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/LogOutput.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/OperatorCommands.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/SystemMonitorOutput.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/SystemStatusOutputs.java");
        inputs.add("inputs/java-ranger-regression/alarm/impl/TopLevelModeOutputs.java");
        frontend.parseFromListOfFile(inputs);
        if (frontend.getParserContext().getExceptions().size() > 0) {
            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
            return;
        }
        //frontend.parseFromFile("inputs/module-info.java");
        //frontend.parseFromFile("inputs/Test.java");
        //Program p = JavaFrontend.parseFromFile("src/main/java/it/unive/jlisa/frontend/JavaFrontend.java");
        Program p = frontend.getProgram();
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = outdir;
        conf.serializeResults = false;
        conf.jsonOutput = false;
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
        conf.callGraph = new RTACallGraph();
        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
        conf.optimize = false;

        FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap().bottom();
        TypeEnvironment<InferredTypes> type = new TypeEnvironment<>(new InferredTypes());
        ValueEnvironment<IntegerConstantPropagation> domain = new ValueEnvironment<>(new IntegerConstantPropagation());
        conf.abstractState = new SimpleAbstractState<>(heap, domain, type);

        LiSA lisa = new LiSA(conf);
        lisa.run(p);
        //CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        /*cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                System.out.println("Method: " + node.getName());
                return super.visit(node);
            }
        });*/
    }
}