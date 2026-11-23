package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaCharType;
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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringGetChars extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringGetChars(
			CFG cfg,
			CodeLocation location,
			Expression[] subExpressions) {
		super(cfg, location, "getChars", subExpressions);
	}

	public static StringGetChars build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringGetChars(cfg, location, params);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	/**
	 * The maximum number of cells of an array that are accessed through
	 * constant indexes (the same of {@code JavaArrayAccess}).
	 */
	private static final int MAX_ENUMERATED_CELLS = 64;

	/**
	 * Yields the integer constant the given expression evaluates to, or
	 * {@code null} if it is not a single constant.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Integer constant(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expression,
			ScopeId<A> context)
			throws SemanticException {
		ExpressionSet rewritten = analysis.rewrite(state, expression, context, originating);
		if (rewritten.size() != 1)
			return null;
		SymbolicExpression e = rewritten.iterator().next();
		if (e instanceof Constant && ((Constant) e).getValue() instanceof Integer)
			return (Integer) ((Constant) e).getValue();
		return null;
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
		SymbolicExpression[] exprs = new SymbolicExpression[params.length];

		for (int i = 0; i < params.length; ++i) {
			ExpressionSet set = params[i];
			if (set.size() > 1 || set.size() <= 0)
				throw new IllegalArgumentException("Number of operands is incorrect!");
			for (SymbolicExpression expr : set) {
				exprs[i] = expr;
			}
		}

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		// the operands are the receiver, the first and the last (excluded)
		// characters to copy, the destination array, and the position in the
		// destination of the first character copied
		Integer from = constant(analysis, state, exprs[1], context);
		Integer to = constant(analysis, state, exprs[2], context);
		Integer at = constant(analysis, state, exprs[4], context);
		boolean exact = from != null && to != null && at != null && from <= to;

		// the receiver is a string or a string builder, whose contents are a
		// string in both cases
		Type stringType = getProgram().getTypes().getStringType();
		Type receiverType = getSubExpressions()[0].getStaticType();
		receiverType = receiverType.isReferenceType() ? receiverType.asReferenceType().getInnerType() : stringType;
		AccessChild contents = new AccessChild(stringType, new HeapDereference(receiverType, exprs[0], getLocation()),
				new GlobalVariable(Untyped.INSTANCE, "value", getLocation()), getLocation());
		HeapDereference dst = new HeapDereference(JavaArrayType.CHAR_ARRAY, exprs[3], getLocation());
		AccessChild length = new AccessChild(Untyped.INSTANCE, dst,
				new Variable(JavaIntType.INSTANCE, "length", getLocation()), getLocation());

		// a null destination raises NullPointerException, and the bounds
		// raise IndexOutOfBoundsException when from < 0, from > to, to is
		// beyond the receiver, at < 0, or the copied characters do not fit
		// the destination
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		state = raising.raiseIfNull(exprs[3], state);
		CodeLocation loc = getLocation();
		Constant zero = new Constant(JavaIntType.INSTANCE, 0, loc);
		AccessChild dstLength = new AccessChild(JavaIntType.INSTANCE, dst,
				new GlobalVariable(JavaIntType.INSTANCE, "length", loc), loc);
		BinaryExpression copied = new BinaryExpression(JavaIntType.INSTANCE, exprs[4],
				new BinaryExpression(JavaIntType.INSTANCE, exprs[2], exprs[1], NumericNonOverflowingSub.INSTANCE, loc),
				NumericNonOverflowingAdd.INSTANCE, loc);
		BinaryExpression outOfBounds = or(loc,
				new BinaryExpression(JavaBooleanType.INSTANCE, exprs[1], zero, ComparisonLt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, exprs[1], exprs[2], ComparisonGt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, exprs[2],
						new UnaryExpression(JavaIntType.INSTANCE, contents, JavaStringLengthOperator.INSTANCE, loc),
						ComparisonGt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, exprs[4], zero, ComparisonLt.INSTANCE, loc),
				new BinaryExpression(JavaBooleanType.INSTANCE, copied, dstLength, ComparisonGt.INSTANCE, loc));
		state = raising.raiseWhere(outOfBounds, true, state, "java.lang.IndexOutOfBoundsException");
		if (state.getExecutionState().isBottom())
			return raising.moveFrom(state.bottomExecution());

		// the cells of the destination are written one by one, as the cells
		// of arrays are accessed through constant indexes when there are few
		// of them (see JavaArrayAccess): a copied character is the one of the
		// receiver if the range is known, and unknown otherwise (cells beyond
		// the enumerated ones are accessed through non-constant indexes, and
		// have unknown contents)
		AnalysisState<A> result = state;
		for (int i = 0; i < MAX_ENUMERATED_CELLS; i++) {
			Constant idx = new Constant(JavaIntType.INSTANCE, i, getLocation());
			BinaryExpression inBounds = new BinaryExpression(JavaBooleanType.INSTANCE, idx, length,
					ComparisonLt.INSTANCE, getLocation());
			if (analysis.satisfies(result, inBounds, context, originating) == Satisfiability.NOT_SATISFIED)
				break;
			SymbolicExpression value;
			if (!exact)
				value = new PushAny(JavaCharType.INSTANCE, getLocation());
			else if (i < at || i >= at + (to - from))
				// not written
				continue;
			else
				value = new BinaryExpression(JavaCharType.INSTANCE, contents,
						new Constant(JavaIntType.INSTANCE, from + i - at, getLocation()),
						JavaStringCharAtOperator.INSTANCE, getLocation());
			result = analysis.assign(result, new AccessChild(JavaCharType.INSTANCE, dst, idx, getLocation()), value,
					context, originating);
		}
		return raising.moveFrom(result.withExecutionExpression(new Skip(getLocation())));
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
}
