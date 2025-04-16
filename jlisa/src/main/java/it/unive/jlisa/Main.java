package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        //String source = "public class Hello { public static void main() { System.out.println(\"Hello World :)\"); } }";

        //ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // NOTE: JLS8 is deprecated. getJLSLatest will return JDK23
        //parser.setSource(source.toCharArray());
        //parser.setKind(ASTParser.K_COMPILATION_UNIT);

        JavaFrontend frontend = new JavaFrontend();
        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/prop8/Main.java");
        frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
        //frontend.parseFromFile("inputs/module-info.java");
        //frontend.parseFromFile("inputs/Test.java");
        //Program p = JavaFrontend.parseFromFile("src/main/java/it/unive/jlisa/frontend/JavaFrontend.java");
        Program p = frontend.getProgram();
        System.out.println(p);
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = "output";
        conf.serializeResults = false;
        conf.jsonOutput = false;
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
        conf.callGraph = new RTACallGraph();
        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
        conf.optimize = false;
        //conf.semanticChecks.add(rosGraphDumper);
        FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap().bottom();
        TypeEnvironment<InferredTypes> type = new TypeEnvironment<>(new InferredTypes());
        ValueEnvironment<IntegerConstantPropagation> domain = new ValueEnvironment<>(new IntegerConstantPropagation());
        conf.abstractState = new SimpleAbstractState<>(heap, domain, type);
        //Program maru = MaruFrontend.processFile("fastapi.mr");

        //PyFrontend translator = new PyFrontend("tests/fastapi/microserviceA.py", false);
        //Program program = translator.toLiSAProgram();

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