package it.unive.jlisa.helpers;

import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.value.ConstantPropagation;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import java.util.ArrayList;

public class TestHelpers {

	public static LiSA getLiSA(
			String outDir) {
		CronConfiguration conf = constantPropagation("", "");
		conf.workdir = outDir;
		return new LiSA(conf);
	}

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
		// conf.analysisGraphs = CronConfiguration.GraphType.HTML_WITH_SUBNODES;
		// conf.semanticChecks.add(new OpenCallsFinder<>());

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new InferredTypes();
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
		CronConfiguration conf = createConfiguration(testDir, subDir, programFiles);

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new InferredTypes();
		ConstantPropagation domain = new ConstantPropagation();
		conf.analysis = new SimpleAbstractDomain<>(heap, domain, type);

		return conf;
	}

	public static CronConfiguration assertCheckerWithConstantPropagation(
			String testDir,
			String subDir,
			String... programFiles) {
		CronConfiguration conf = createConfiguration(testDir, subDir, programFiles);

		conf.semanticChecks.add(new AssertChecker());

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new JavaFieldSensitivePointBasedHeap();
		InferredTypes type = new InferredTypes();
		ConstantPropagation domain = new ConstantPropagation();
		conf.analysis = new SimpleAbstractDomain<>(heap, domain, type);

		return conf;
	}
}