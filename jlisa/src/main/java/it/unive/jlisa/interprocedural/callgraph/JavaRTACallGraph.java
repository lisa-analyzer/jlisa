package it.unive.jlisa.interprocedural.callgraph;

import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

public class JavaRTACallGraph extends JavaCallGraph {
	@Override
	public Collection<Type> getPossibleTypesOfReceiver(
			Expression receiver,
			Set<Type> types)
			throws CallResolutionException {
		return types;
	}
}
