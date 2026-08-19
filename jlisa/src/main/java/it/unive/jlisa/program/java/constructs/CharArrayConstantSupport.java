package it.unive.jlisa.program.java.constructs;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Shared best-effort constant reconstruction helpers, used by constructs that
 * consume {@code char[]} arguments (e.g. {@code String.valueOf(char[],
 * ...)}, {@code StringBuilder.insert(int, char[], ...)}) to recover the
 * concrete content of a char array from the underlying value domain, so that
 * the resulting String content can be tracked precisely instead of collapsing
 * to top.
 */
public final class CharArrayConstantSupport {

	private CharArrayConstantSupport() {
	}

	// reads count characters starting at offset from the array referenced by
	// arrayRef, or returns null as soon as either arrayRef is not an array
	// reference or one of the cells in range cannot be resolved to a single
	// constant value
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> String computeConstantSubstring(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression arrayRef,
			int offset,
			int count,
			CodeLocation location,
			ProgramPoint pp)
			throws SemanticException {
		if (!arrayRef.getStaticType().isReferenceType()
				|| !arrayRef.getStaticType().asReferenceType().getInnerType().isArrayType())
			return null;

		JavaArrayType arrayType = (JavaArrayType) arrayRef.getStaticType().asReferenceType().getInnerType();
		HeapDereference container = new HeapDereference(arrayType, arrayRef, location);
		Type accessType = arrayType.getInnerType();

		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < offset + count; i++) {
			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);
			AccessChild access = new AccessChild(accessType, container, idx, location);
			Object c = extractConstantValue(interprocedural, state, access, pp);
			if (!(c instanceof Integer))
				return null;
			sb.append((char) (int) (Integer) c);
		}
		return sb.toString();
	}

	// resolves the (constant) length of the array referenced by arrayRef, or
	// null if it is not an array reference or its length is not known
	// precisely
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Integer extractArrayLength(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression arrayRef,
			CodeLocation location,
			ProgramPoint pp)
			throws SemanticException {
		if (!arrayRef.getStaticType().isReferenceType()
				|| !arrayRef.getStaticType().asReferenceType().getInnerType().isArrayType())
			return null;

		JavaArrayType arrayType = (JavaArrayType) arrayRef.getStaticType().asReferenceType().getInnerType();
		HeapDereference container = new HeapDereference(arrayType, arrayRef, location);
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", location);
		AccessChild lenAccess = new AccessChild(Untyped.INSTANCE, container, lenProperty, location);

		Object length = extractConstantValue(interprocedural, state, lenAccess, pp);
		return length instanceof Integer ? (Integer) length : null;
	}

	// resolves expr to a single constant value tracked by the underlying
	// value domain, or null if it cannot be resolved to exactly one constant
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Object extractConstantValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			ProgramPoint pp)
			throws SemanticException {
		Stream<BinaryExpression> constraints = extractConstraints(interprocedural, state, expr, pp);
		if (constraints == null)
			return null;

		List<BinaryExpression> list = constraints.toList();
		if (list.isEmpty())
			return null;

		Object value = null;
		for (BinaryExpression constraint : list) {
			if (!(constraint.getLeft() instanceof Constant c))
				return null;
			if (value == null)
				value = c.getValue();
			else if (!value.equals(c.getValue()))
				// the cell is not resolved to a single constant value
				return null;
		}
		return value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					ProgramPoint pp)
					throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		SimpleAbstractDomain<?, ?, ?> innerDomain;

		try {
			Class<?> c = Reachability.class;
			Field f = c.getDeclaredField("domain");

			f.setAccessible(true);

			innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);
		} catch (Exception e) {
			return null;
		}

		assert (innerDomain != null);
		ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

		Object executionState = state.getExecutionState();
		ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

		SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

		ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

		SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

		ExpressionSet rewritten = analysis.rewrite(state, expr, pp);

		return StreamSupport.stream(rewritten.spliterator(), false)
				.map(ex -> (ValueExpression) ex)
				.flatMap(vex -> {
					try {
						Set<BinaryExpression> c = vdom.constraints(null, env, vex, pp, oracle);
						// a null result means the value is bottom (unreachable
						// cell): treat it as unresolved rather than crashing
						return c == null ? Stream.<BinaryExpression>empty() : c.stream();
					} catch (SemanticException e) {
						return Stream.<BinaryExpression>empty();
					}
				});
	}
}
