package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
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
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The method {@code containsKey(key)} of the maps: see
 * {@link MapModel#containsKey}. With a constant key, the result is unknown
 * (false for an empty map); a map that is not tracked gives an unknown result.
 */
public class MapContainsKey extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapContainsKey(
			CFG cfg,
			CodeLocation location,
			Expression map,
			Expression key) {
		super(cfg, location, "containsKey", map, key);
	}

	public static MapContainsKey build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapContainsKey(cfg, location, params[0], params[1]);
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
		// a ConcurrentMap is not null safe
		return MapModel.nullKey(interprocedural, state, semantics(interprocedural, state, left, right, expressions),
				left,
				right, this, getSubExpressions(), expressions);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> semantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		if (!MapModel.isTracked(analysis, context, state, left, this))
			return analysis.smallStepSemantics(state, new PushAny(JavaBooleanType.INSTANCE, location), context, this);

		// the empty map has no keys
		AccessChild size = MapModel.size(left, location);
		AnalysisState<A> empty = analysis.assume(state, new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, size, new Constant(JavaIntType.INSTANCE, 0, location),
				ComparisonEq.INSTANCE, location), context, this, this);
		AnalysisState<A> nonEmpty = analysis.assume(state, new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, new Constant(JavaIntType.INSTANCE, 0, location), size,
				ComparisonLt.INSTANCE, location), context, this, this);
		AnalysisState<A> result = empty.getExecutionState().isBottom() ? state.bottomExecution()
				: analysis.smallStepSemantics(empty, new Constant(JavaBooleanType.INSTANCE, false, location), context,
						this);
		if (nonEmpty.getExecutionState().isBottom())
			return result;
		String constant = constantKey(interprocedural, nonEmpty, right);
		if (constant == null)
			return MapModel.single(analysis, context,
					result.lub(MapModel.containsKey(interprocedural, nonEmpty, left, right, this)),
					JavaBooleanType.INSTANCE, this);
		return MapModel.single(analysis, context,
				result.lub(analysis.smallStepSemantics(nonEmpty, new PushAny(JavaBooleanType.INSTANCE, location),
						context, this)),
				JavaBooleanType.INSTANCE, this);
	}

	// the constant value of the key (an int or a string with a single value),
	// that maps write in a field named after it (see MapPut), or null
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> String constantKey(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression key)
			throws SemanticException {
		Set<Type> types = interprocedural.getAnalysis().getRuntimeTypesOf(state, key,
				interprocedural.getCurrentContext(), this);
		if (types.size() != 1)
			return null;
		Type type = types.iterator().next();
		if (type instanceof JavaReferenceType jrt)
			type = jrt.getInnerType();
		SymbolicExpression value = key;
		if (type.isStringType())
			value = new AccessChild(type, new HeapDereference(type, key, getLocation()),
					new GlobalVariable(Untyped.INSTANCE, "value", getLocation()), getLocation());
		else if (type != JavaIntType.INSTANCE)
			return null;
		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = ConstraintUtils
				.extractConstraints(interprocedural, state, value, this);
		if (constraints == null)
			return null;
		List<it.unive.lisa.symbolic.value.BinaryExpression> list = constraints.toList();
		if (list.size() != 1 || !(list.get(0).getLeft() instanceof Constant c))
			return null;
		return c.getValue().toString();
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}
