package it.unive.jlisa.helpers;

import java.util.ArrayList;

import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;

public class TestHelpers {
    
	/**
	 * Creates and returns a {@link CronConfiguration} instance for running JLiSA cron tests.
	 * 
	 * @param testDir the base directory containing the tests
	 * @param subDir the subdirectory within the test directory containing specific test files
	 * @param programFiles the names of the program files to be analyzed
	 * @return a configured {@code CronConfiguration} instance ready for analysis
	 */
	public static CronConfiguration createConfiguration(String testDir, String subDir, String... programFiles) {

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
//		conf.analysisGraphs = GraphType.HTML_WITH_SUBNODES;

		// the abstract domain
		FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap();
		TypeEnvironment<InferredTypes> type = new TypeEnvironment<>(new InferredTypes());
		ValueEnvironment<Interval> domain = new ValueEnvironment<>(new Interval());

		conf.abstractState = new SimpleAbstractState<>(heap, domain, type);
		
		// for interprocedural analysis
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		return conf;
	}
}
