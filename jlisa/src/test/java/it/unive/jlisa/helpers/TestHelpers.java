package it.unive.jlisa.helpers;

import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagation;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import java.util.ArrayList;

public class TestHelpers {

	/**
	 * Creates and returns a {@link CronConfiguration} instance for running
	 * JLiSA cron tests.
	 * 
	 * @param testDir      the base directory containing the tests
	 * @param subDir       the subdirectory within the test directory containing
	 *                         specific test files
	 * @param programFiles the names of the program files to be analyzed
	 * 
	 * @return a configured {@code CronConfiguration} instance ready for
	 *             analysis
	 */
	public static CronConfiguration createConfiguration(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = testDir;
		conf.testSubDir = subDir;
		conf.programFiles = new ArrayList<>();
		for (String pf : programFiles)
			conf.programFiles.add(pf);
		conf.serializeResults = true;
		conf.jsonOutput = false;
		conf.optimize = false;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		// conf.forceUpdate = true;
		// conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new JavaInferredTypes();
		Interval domain = new Interval();

		conf.analysis = new SimpleAbstractDomain<>(heap, domain, type);

		// for interprocedural analysis
		conf.callGraph = new JavaRTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		return conf;
	}

	public static CronConfiguration constantPropagation(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = testDir;
		conf.testSubDir = subDir;
		conf.programFiles = new ArrayList<>();
		for (String pf : programFiles)
			conf.programFiles.add(pf);
		conf.serializeResults = true;
		conf.jsonOutput = false;
		conf.optimize = false;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
//		 conf.forceUpdate = true;
//		 conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new JavaInferredTypes();
		ConstantPropagation domain = new ConstantPropagation();

		conf.analysis = new SimpleAbstractDomain<>(heap, domain, type);

		// for interprocedural analysis
		conf.callGraph = new JavaRTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		return conf;
	}

	public static CronConfiguration assertCheckerWithConstantPropagation(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = testDir;
		conf.testSubDir = subDir;
		conf.programFiles = new ArrayList<>();
		for (String pf : programFiles)
			conf.programFiles.add(pf);
		conf.serializeResults = true;
		conf.jsonOutput = false;
		conf.optimize = false;
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.semanticChecks.add(new AssertChecker());
//		conf.forceUpdate = true;
//		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new JavaInferredTypes();
		ConstantPropagation domain = new ConstantPropagation();

		conf.analysis = new SimpleAbstractDomain<>(heap, domain, type);

		// for interprocedural analysis
		conf.callGraph = new JavaRTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		return conf;
	}
}
