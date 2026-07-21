package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
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
import java.util.Set;
import java.util.stream.Collectors;

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
		// to call the one of its ancestors (a superclass for a ClassUnit, or a
		// superinterface for an InterfaceUnit)
		CompilationUnit definingUnit = JavaClassType.hasType(getQualifier())
				? JavaClassType.lookup(getQualifier()).getUnit()
				: JavaInterfaceType.lookup(getQualifier()).getUnit();
		if (definingUnit.getCodeMembersByName(getTargetName()).isEmpty()) {
			Class<? extends CompilationUnit> containerKind = definingUnit.getClass();
			Set<CompilationUnit> ancestors = definingUnit
					.getImmediateAncestors().stream()
					.filter(u -> u.getClass() == containerKind)
					.collect(Collectors.toSet());

			// we can safely suppose that there exist a single ancestor
			// declaring the target; if none is found (e.g., classUnit is
			// Object), we keep the original unit
			definingUnit = ancestors.stream().findFirst().orElse(definingUnit);
		}

		JavaReferenceType definingUnitType = definingUnit instanceof ClassUnit
				? JavaClassType.lookup(definingUnit.getName()).getReference()
				: JavaInterfaceType.lookup(definingUnit.getName()).getReference();
		state = InitializedClassSet.initialize(state, definingUnitType, this, interprocedural);

		UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), Call.CallType.STATIC,
				definingUnit.toString(),
				getTargetName(), getParameters());
		call.setSource(this);
		AnalysisState<A> callState = call.forwardSemantics(state, interprocedural, expressions);
		getMetaVariables().addAll(call.getMetaVariables());
		return callState;
	}
}
