package it.unive.jlisa.program.cfg.expression;

import java.util.Set;
import java.util.stream.Collectors;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;

public class UnresolvedSuperCall extends UnresolvedCall {

	public UnresolvedSuperCall(CFG cfg, SourceCodeLocation location, CallType instance, String qualifier, String name,
			Expression[] parameters) {
		super(cfg, location, instance, qualifier, name, parameters);
	}
	
	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, ExpressionSet[] params,
			StatementStore<A> expressions) throws SemanticException {
		ClassUnit superClass = (ClassUnit) JavaClassType.lookup(getQualifier(), null).getUnit();
		boolean resolved = false;

		do {
			Set<it.unive.lisa.program.CompilationUnit> superClasses = superClass
					.getImmediateAncestors().stream()
					.filter(u -> u instanceof ClassUnit)
					.collect(Collectors.toSet());

			if (superClasses.size() > 1)
				throw new SemanticException("A class must have one super class");

			superClass = (ClassUnit) superClasses.stream().findFirst().get();
			if (!superClass.getInstanceCodeMembersByName(getTargetName(), false).isEmpty()) {
				resolved = true;
				break;
			}
		} while (!superClass.getName().equals("Object"));

		if (!resolved)
			throw new SemanticException("Cannot resolved super method invocation");

		UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), Call.CallType.INSTANCE, superClass.getName(), getTargetName(), getSubExpressions());
		return call.forwardSemantics(state, interprocedural, expressions);
	}

}
