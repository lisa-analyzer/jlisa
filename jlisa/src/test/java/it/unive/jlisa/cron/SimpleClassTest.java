package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class SimpleClassTest extends JLiSAAnalysisExecutor {
	
	@Test
	public void simpleClassTes1()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("test01", "", "Main.java", "B.java");
		perform(conf);
	}
	
	@Test
	public void simpleClassTes2()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("test02", "", "Main.java");
		perform(conf);
	}
	
	@Test
	public void simpleClassTes3()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("test03", "", "Test.java");
		perform(conf);
	}
	
//    @Test
//    void simpleClassTestold() throws IOException {
//        JavaFrontend frontend = new JavaFrontend();
//        String outdir = "outputs/test/";
//        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/prop8/Main.java");
//        //
//        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/DW.java");
//        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/Divs32.java");
//        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/AlarmFunctional.java");
//        //frontend.parseFromFile("inputs/java-ranger-regression/alarm/impl/B.java");
//        List<String> inputs = new ArrayList<>();
//        //inputs.add("inputs/Test.java");
//        inputs.add("inputs/test01/Main.java");
//        inputs.add("inputs/test01/B.java");
//        //inputs.add("inputs/test02/B.java");
//        frontend.parseFromListOfFile(inputs);
// 
//        //frontend.parseFromFile("inputs/module-info.java");
//        //frontend.parseFromFile("inputs/Test.java");
//        //Program p = JavaFrontend.parseFromFile("src/main/java/it/unive/jlisa/frontend/JavaFrontend.java");
//        Program p = frontend.getProgram();
//        LiSAConfiguration conf = new LiSAConfiguration();
//        conf.workdir = outdir;
//        conf.serializeResults = false;
//        conf.jsonOutput = false;
//        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
//        conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
//        conf.callGraph = new RTACallGraph();
//        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
//        conf.optimize = false;
//
//        FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap().bottom();
//        TypeEnvironment<InferredTypes> type = new TypeEnvironment<>(new InferredTypes());
//        ValueEnvironment<IntegerConstantPropagation> domain = new ValueEnvironment<>(new IntegerConstantPropagation());
//        conf.abstractState = new SimpleAbstractState<>(heap, domain, type);
//
//        LiSA lisa = new LiSA(conf);
//        lisa.run(p);
//        //CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//        /*cu.accept(new ASTVisitor() {
//            public boolean visit(MethodDeclaration node) {
//                System.out.println("Method: " + node.getName());
//                return super.visit(node);
//            }
//        });*/
//    }
}