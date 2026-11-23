package it.unive.jlisa.program.java.constructs.system;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link System#arraycopy(Object, int, Object, int, int)}: copies
 * {@code length} cells of {@code src}, from {@code srcPos}, to the cells of
 * {@code dest} from {@code destPos}. It raises {@link NullPointerException} if
 * an array is {@code null}, {@link ArrayIndexOutOfBoundsException} if a
 * position or the length is negative or the copied cells exceed an array, and
 * {@link ArrayStoreException} if the arrays have incompatible types. The cells
 * of the destination are copied exactly when the positions and the length are
 * known, and become unknown otherwise.
 */
public class SystemArrayCopy extends NaryExpression implements PluggableStatement {

	// the cells of arrays that are tracked one by one (see JavaArrayAccess)
	private static final int MAX_ENUMERATED_CELLS = 64;

	/**
	 * The call implemented by this native.
	 */
	protected Statement originating;

	/**
	 * Builds the native.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param params   the parameters ({@code src}, {@code srcPos},
	 *                     {@code dest}, {@code destPos}, {@code length})
	 */
	public SystemArrayCopy(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, "arraycopy", params);
	}

	/**
	 * Builds the native.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param params   the parameters
	 *
	 * @return the native
	 */
	public static SystemArrayCopy build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SystemArrayCopy(cfg, location, params);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		AnalysisState<A> result = combinations(interprocedural, state, params, new SymbolicExpression[5], 0,
				raising);
		return raising.moveFrom(result);
	}

	// evaluates each combination of the operands in the executions computing
	// it
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> combinations(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			SymbolicExpression[] args,
			int pos,
			JavaExceptions.Raising<A, D> raising)
			throws SemanticException {
		if (pos == params.length)
			return copy(interprocedural, state, args, raising);
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression e : params[pos]) {
			args[pos] = e;
			AnalysisState<A> computing = interprocedural.getAnalysis().restrictTo(state, getSubExpressions()[pos], e);
			if (!computing.getExecutionState().isBottom())
				result = result.lub(combinations(interprocedural, computing, params, args, pos + 1, raising));
		}
		return result;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> copy(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression[] args,
			JavaExceptions.Raising<A, D> raising)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation loc = getLocation();
		SymbolicExpression src = args[0], srcPos = args[1], dest = args[2], destPos = args[3], length = args[4];

		// null arrays
		state = raising.raiseIfNull(src, state);
		state = raising.raiseIfNull(dest, state);
		if (state.getExecutionState().isBottom())
			return state;

		// the types of the arrays: the copy is checked (and performed) only
		// if each is a single array type, and the cells are stored only if
		// the types are the same
		JavaArrayType srcType = arrayType(analysis.getRuntimeTypesOf(state, src, context, this));
		JavaArrayType destType = arrayType(analysis.getRuntimeTypesOf(state, dest, context, this));
		if (srcType == null || destType == null || !srcType.equals(destType))
			raising.raise(Satisfiability.UNKNOWN, state, "java.lang.ArrayStoreException");
		if (srcType == null || destType == null) {
			// the arrays are not known: the indexes might be out of bounds
			raising.raise(Satisfiability.UNKNOWN, state, "java.lang.ArrayIndexOutOfBoundsException");
			return state.withExecutionExpression(new Skip(loc));
		}

		HeapDereference srcArray = new HeapDereference(srcType, src, loc);
		HeapDereference destArray = new HeapDereference(destType, dest, loc);
		GlobalVariable lengthField = new GlobalVariable(JavaIntType.INSTANCE, "length", loc);
		AccessChild srcLength = new AccessChild(JavaIntType.INSTANCE, srcArray, lengthField, loc);
		AccessChild destLength = new AccessChild(JavaIntType.INSTANCE, destArray, lengthField, loc);
		Constant zero = new Constant(JavaIntType.INSTANCE, 0, loc);
		BinaryExpression outOfBounds = or(loc,
				new BinaryExpression(JavaBooleanType.INSTANCE, srcPos, zero, ComparisonLt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, destPos, zero, ComparisonLt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, length, zero, ComparisonLt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE,
						new BinaryExpression(JavaIntType.INSTANCE, srcPos, length, NumericNonOverflowingAdd.INSTANCE,
								loc),
						srcLength, ComparisonGt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE,
						new BinaryExpression(JavaIntType.INSTANCE, destPos, length, NumericNonOverflowingAdd.INSTANCE,
								loc),
						destLength, ComparisonGt.INSTANCE, loc));
		state = raising.raiseWhere(outOfBounds, true, state, "java.lang.ArrayIndexOutOfBoundsException");
		if (state.getExecutionState().isBottom())
			return state;

		// the cells of the destination: the copied ones if the range is
		// known, any value otherwise
		Integer from = constant(analysis, state, srcPos, context);
		Integer at = constant(analysis, state, destPos, context);
		Integer count = constant(analysis, state, length, context);
		boolean exact = from != null && at != null && count != null;
		Type content = destType.getInnerType();
		AnalysisState<A> result = state;
		for (int i = 0; i < MAX_ENUMERATED_CELLS; i++) {
			Constant idx = new Constant(JavaIntType.INSTANCE, i, loc);
			if (analysis.satisfies(result, new BinaryExpression(JavaBooleanType.INSTANCE, idx, destLength,
					ComparisonLt.INSTANCE, loc), context, this) == Satisfiability.NOT_SATISFIED)
				break;
			AccessChild cell = new AccessChild(content, destArray, idx, loc);
			if (exact) {
				if (i < at || i >= at + count)
					continue;
				result = analysis.assign(result, cell,
						new AccessChild(content, srcArray, new Constant(JavaIntType.INSTANCE, from + i - at, loc),
								loc),
						context, this);
			} else {
				AnalysisState<A> any = analysis.assign(result, cell, new PushAny(content, loc), context, this);
				if (content.isReferenceType() || content.isInMemoryType())
					any = any.lub(analysis.assign(result, cell, new NullConstant(loc), context, this));
				result = any;
			}
		}
		return result.withExecutionExpression(new Skip(loc));
	}

	// the array type among the given runtime types (null excluded), if it is
	// the only one
	private static JavaArrayType arrayType(
			Set<Type> types) {
		Set<Type> arrays = new HashSet<>();
		for (Type t : types)
			if (JavaExceptions.isNull(t))
				continue;
			else if (t.isReferenceType() && t.asReferenceType().getInnerType() instanceof JavaArrayType)
				arrays.add(t.asReferenceType().getInnerType());
			else
				return null;
		return arrays.size() == 1 ? (JavaArrayType) arrays.iterator().next() : null;
	}

	// the disjunction of the given conditions
	private static BinaryExpression or(
			CodeLocation location,
			BinaryExpression first,
			BinaryExpression... others) {
		BinaryExpression result = first;
		for (BinaryExpression other : others)
			result = new BinaryExpression(JavaBooleanType.INSTANCE, result, other, LogicalOr.INSTANCE, location);
		return result;
	}

	// the value of the given expression, if it is a constant int
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Integer constant(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expression,
			ScopeId<A> context)
			throws SemanticException {
		ExpressionSet rewritten = analysis.rewrite(state, expression, context, this);
		if (rewritten.size() != 1)
			return null;
		SymbolicExpression e = rewritten.iterator().next();
		if (e instanceof Constant && ((Constant) e).getValue() instanceof Integer)
			return (Integer) ((Constant) e).getValue();
		return null;
	}
}
