package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.triage.model.SummaryMarker;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * The methods of {@link Integer} and {@link Long} reading a system property as
 * a number, with a default value (e.g.,
 * {@link Integer#getInteger(String, int)}). System properties are not modeled,
 * but the names of the properties defined by Java runtimes contain dots (e.g.,
 * {@code java.version}): if the name contains no dot, the property is assumed
 * not to be defined (as it happens also when the name is empty), and the result
 * is the default value. Otherwise, the result is either the default value or an
 * unknown number (that is never a precise value, see {@link SummaryMarker}).
 */
public abstract class PropertyLookup extends BinaryExpression implements PluggableStatement {

	protected Statement originating;

	private final String wrapper;

	private final Type wrapped;

	private final boolean boxedDefault;

	protected PropertyLookup(
			CFG cfg,
			CodeLocation location,
			String name,
			String wrapper,
			Type wrapped,
			boolean boxedDefault,
			Expression property,
			Expression def) {
		super(cfg, location, name, property, def);
		this.wrapper = wrapper;
		this.wrapped = wrapped;
		this.boxedDefault = boxedDefault;
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type stringType = JavaClassType.getStringType();
		Type boolType = getProgram().getTypes().getBooleanType();
		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		SymbolicExpression name = new AccessChild(stringType, new HeapDereference(stringType, left, getLocation()),
				value, getLocation());
		SymbolicExpression dotted = new it.unive.lisa.symbolic.value.BinaryExpression(boolType, name,
				new Constant(stringType, ".", getLocation()), StringContains.INSTANCE, getLocation());

		AnalysisState<A> undefined = analysis.assume(state,
				new UnaryExpression(boolType, dotted, LogicalNegation.INSTANCE, getLocation()), context, this, this);
		AnalysisState<A> unknown = analysis.assume(state, dotted, context, this, this);

		AnalysisState<A> result = state.bottomExecution();
		if (!undefined.getExecution().isBottom())
			result = result.lub(def(interprocedural, undefined, right, expressions));
		if (!unknown.getExecution().isBottom()) {
			result = result.lub(def(interprocedural, unknown, right, expressions));
			SummaryMarker read = new SummaryMarker(wrapped, "value of a system property", getLocation());
			result = result.lub(box(interprocedural, unknown, read, expressions));
		}
		return result;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> def(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression def,
			StatementStore<A> expressions)
			throws SemanticException {
		if (boxedDefault)
			// the default value is returned as it is
			return interprocedural.getAnalysis().smallStepSemantics(state, def,
					interprocedural.getCurrentContext(), originating);
		return box(interprocedural, state, def, expressions);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> box(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression v,
			StatementStore<A> expressions)
			throws SemanticException {
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(JavaClassType.lookup(wrapper)), new Expression[] { getRight() });
		AnalysisState<A> boxed = call.forwardSemanticsAux(interprocedural, state,
				new ExpressionSet[] { new ExpressionSet(v) }, expressions);
		getMetaVariables().addAll(call.getMetaVariables());
		return boxed;
	}

	public static class IntegerWithInt extends PropertyLookup {

		public IntegerWithInt(
				CFG cfg,
				CodeLocation location,
				Expression property,
				Expression def) {
			super(cfg, location, "getInteger", "java.lang.Integer", JavaIntType.INSTANCE, false, property, def);
		}

		public static IntegerWithInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerWithInt(cfg, location, params[0], params[1]);
		}
	}

	public static class IntegerWithInteger extends PropertyLookup {

		public IntegerWithInteger(
				CFG cfg,
				CodeLocation location,
				Expression property,
				Expression def) {
			super(cfg, location, "getInteger", "java.lang.Integer", JavaIntType.INSTANCE, true, property, def);
		}

		public static IntegerWithInteger build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerWithInteger(cfg, location, params[0], params[1]);
		}
	}

	public static class LongWithLong extends PropertyLookup {

		public LongWithLong(
				CFG cfg,
				CodeLocation location,
				Expression property,
				Expression def) {
			super(cfg, location, "getLong", "java.lang.Long", JavaLongType.INSTANCE, false, property, def);
		}

		public static LongWithLong build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongWithLong(cfg, location, params[0], params[1]);
		}
	}

	public static class LongWithBoxed extends PropertyLookup {

		public LongWithBoxed(
				CFG cfg,
				CodeLocation location,
				Expression property,
				Expression def) {
			super(cfg, location, "getLong", "java.lang.Long", JavaLongType.INSTANCE, true, property, def);
		}

		public static LongWithBoxed build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongWithBoxed(cfg, location, params[0], params[1]);
		}
	}
}
