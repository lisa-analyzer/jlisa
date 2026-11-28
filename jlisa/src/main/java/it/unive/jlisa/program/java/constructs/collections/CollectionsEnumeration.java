package it.unive.jlisa.program.java.constructs.collections;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.type.*;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

public class CollectionsEnumeration
		extends
		UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the enumeration.
	 */
	private final CodeLocation enumerationLocation;

	public CollectionsEnumeration(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "enumeration", arg);
		enumerationLocation = new SyntheticCodeLocationManager("enumeration-" + location.toString()).nextLocation();
	}

	public static CollectionsEnumeration build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new CollectionsEnumeration(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression arg,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaClassType enumerationType = JavaClassType.lookup("java.util.Enumeration");
		if (!ListModel.isModeled(analysis, context, state, arg, this))
			return analysis.smallStepSemantics(state, new PushAny(enumerationType.getReference(), getLocation()),
					context, originating);

		// the elements in order for a list, in any order otherwise (e.g., a
		// set)
		boolean list = true;
		for (Type t : analysis.getRuntimeTypesOf(state, arg, context, this)) {
			Type inner = t.isPointerType() ? t.asPointerType().getInnerType() : t;
			String name = inner.toString();
			if (!name.equals("java.util.List") && !name.equals("java.util.ArrayList")
					&& !name.equals("java.util.LinkedList"))
				list = false;
		}
		return ListModel.iterate(analysis, context, state, arg, enumerationType, enumerationLocation, !list, this);
	}
}
