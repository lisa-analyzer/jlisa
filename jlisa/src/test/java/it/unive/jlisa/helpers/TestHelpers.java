package it.unive.jlisa.helpers;

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

public class TestHelpers {

    public static LiSA getLiSA(String outdir) {
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

        return new LiSA(conf);
    }
}
