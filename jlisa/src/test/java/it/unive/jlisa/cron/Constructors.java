package it.unive.jlisa.cron;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import it.unive.jlisa.helpers.CronConfiguration;
import it.unive.jlisa.helpers.JLiSAAnalysisExecutor;
import it.unive.jlisa.helpers.TestHelpers;

public class Constructors extends JLiSAAnalysisExecutor {

	@Test
	public void testThisConstructor()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "this-constructors", "Main.java", "B.java", "C.java");
		perform(conf);
	}
	
//    @Test
//    public void testThisConstructor() throws IOException {
//        String outdir = "tests-outputs/this-constructor/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/this-constructors/Main.java");
//        inputs.add("tests/constructors/this-constructors/B.java");
//        inputs.add("tests/constructors/this-constructors/C.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }

	@Test
	public void testSuperConstructor1()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-1", "Main.java", "B.java");
		perform(conf);
	}
	
//    @Test
//    public void testSuperConstructor1() throws IOException {
//        String outdir = "tests-outputs/super-constructor/test-1/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/super-constructor/test-1/Main.java");
//        inputs.add("tests/constructors/super-constructor/test-1/B.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }

	@Test
	public void testSuperConstructor2()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-2", "Main.java", "B.java");
		perform(conf);
	}
	
//    @Test
//    public void testSuperConstructor2() throws IOException {
//        String outdir = "tests-outputs/super-constructor/test-2/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/super-constructor/test-2/Main.java");
//        inputs.add("tests/constructors/super-constructor/test-2/B.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }

	@Test
	public void testSuperConstructor3()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-3", "Main.java", "B.java", "A.java");
		perform(conf);
	}
	
//    @Test
//    public void testSuperConstructor3() throws IOException {
//        String outdir = "tests-outputs/super-constructor/test-3/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/super-constructor/test-3/Main.java");
//        inputs.add("tests/constructors/super-constructor/test-3/B.java");
//        inputs.add("tests/constructors/super-constructor/test-3/A.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }

	
	@Test
	public void testSuperConstructor4()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "super-constructor/test-4", "Main.java", "B.java", "A.java");
		perform(conf);
	}
	
//    @Test
//    public void testDefaultConstructor4() throws IOException {
//        String outdir = "tests-outputs/super-constructor/test-4/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/super-constructor/test-4/Main.java");
//        inputs.add("tests/constructors/super-constructor/test-4/B.java");
//        inputs.add("tests/constructors/super-constructor/test-4/A.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }

	@Test
	public void testDefaultConstructor1()  throws IOException {
		// FIXME: need to compare the dots and update the ground truth
		CronConfiguration conf = TestHelpers.createConfiguration("constructors", "default-constructor", "Main.java", "B.java");
		perform(conf);
	}
	
//    @Test
//    public void testDefaultConstructor1() throws IOException {
//        String outdir = "tests-outputs/default-constructor/1/";
//        LiSA lisa = TestHelpers.getLiSA(outdir);
//        JavaFrontend frontend = new JavaFrontend();
//
//        List<String> inputs = new ArrayList<>();
//        inputs.add("tests/constructors/default-constructor/Main.java");
//        inputs.add("tests/constructors/default-constructor/B.java");
//        frontend.parseFromListOfFile(inputs);
//
//        if (!frontend.getParserContext().getExceptions().isEmpty()) {
//            CSVExceptionWriter.writeCSV(outdir + "errors.csv", frontend.getParserContext().getExceptions());
//            System.out.println("Some errors occurred. Check " + outdir + "errors.csv file.");
//            return;
//        }
//        lisa.run(frontend.getProgram());
//    }
}
