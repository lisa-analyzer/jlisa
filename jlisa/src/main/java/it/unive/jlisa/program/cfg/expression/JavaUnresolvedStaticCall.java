package it.unive.jlisa.program.cfg.expression;

import java.util.Set;
import java.util.stream.Collectors;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Untyped;

public class JavaUnresolvedStaticCall extends UnresolvedCall {

	public JavaUnresolvedStaticCall(
			CFG cfg,
			CodeLocation location,
			String qualifier,
			String targetName,
			Expression... parameters) {
		super(cfg, location, Call.CallType.STATIC, qualifier, targetName, Untyped.INSTANCE, parameters);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		if (state.getExecutionState().isBottom())
			return state;

		// we need to check whether to call the clinit of the container unit or
		// to call the one of its superclass
		ClassUnit classUnit = (ClassUnit) JavaClassType.lookup(getQualifier()).getUnit();
		if (classUnit.getCodeMembersByName(getTargetName()).isEmpty()) {
			Set<CompilationUnit> superClasses = classUnit
					.getImmediateAncestors().stream()
					.filter(u -> u instanceof ClassUnit)
					.collect(Collectors.toSet());

			// we can safely suppose that there exist a single superclass
			// if classUnit is Object, we keep object
			classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
		}

		state = InitializedClassSet.initialize(state, JavaClassType.lookup(classUnit.getName()).getReference(), this, interprocedural);

		UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), Call.CallType.STATIC, classUnit.toString(),
				getTargetName(), getParameters());
		call.setSource(this);
		AnalysisState<A> callState = call.forwardSemantics(state, interprocedural, expressions);
		getMetaVariables().addAll(call.getMetaVariables());
		return callState;
	}
}
