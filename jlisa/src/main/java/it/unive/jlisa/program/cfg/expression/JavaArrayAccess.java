package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.cfg.statement.JavaCompoundAssignment;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.List;

public class JavaArrayAccess extends BinaryExpression {

	// the number of cells of an array that are tracked: a read with an index
	// that is not a known constant joins these cells only
	// TODO: move to a configuration file
	private static final int MAX_CONST_ARRAY_SIZE = 32;

	public JavaArrayAccess(
			CFG cfg,
			Type staticType,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "[]", staticType, left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (!left.getStaticType().isReferenceType()
				|| !left.getStaticType().asReferenceType().getInnerType().isArrayType())
			return state.bottomExecution();

		if (getSubExpressions()[0] instanceof JavaArrayAccess
				|| getSubExpressions()[1] instanceof JavaArrayAccess) {
			// TODO 99% of these require a more refined heap model, otherwise we
			// have some false
			// positives. For now, we just avoid nested array accesses.
			throw new SemanticException("Nested array accesses are not supported yet");
		}

		// need to check in-bound
		JavaArrayType arrayType = (JavaArrayType) ((JavaReferenceType) left.getStaticType()).getInnerType();
		HeapDereference container = new HeapDereference(arrayType, left, getLocation());
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", getLocation());
		AccessChild lenAccess = new AccessChild(Untyped.INSTANCE, container, lenProperty, getLocation());
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		it.unive.lisa.symbolic.value.BinaryExpression idxCheck1 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, new Constant(JavaIntType.INSTANCE, 0, getLocation()), ComparisonLt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck2 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, lenAccess, ComparisonGe.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression or = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				idxCheck1, idxCheck2, LogicalOr.INSTANCE, getLocation());

		Satisfiability sat = analysis.satisfies(state, or, this);
		if (sat == Satisfiability.SATISFIED) {
			// builds the exception
			JavaClassType oonExc = JavaClassType.getArrayIndexOutOfBoundsExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					oonExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
			AnalysisState<A> exceptionState = state.bottomExecution();

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), oonExc.getReference(), getLocation());
				AnalysisState<A> tmp = analysis.assign(state, throwVar, th, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oonExc.getReference(), this), this));
			}

			return exceptionState;
		} else if (sat == Satisfiability.NOT_SATISFIED) {
			Type accessType = arrayType.getInnerType();
			accessType = accessType.isArrayType() ? accessType.asArrayType().getInnerType() : accessType;
			return readArrayElement(analysis, state, container, right, lenAccess, accessType);
		} else if (sat == Satisfiability.BOTTOM) {
			return state.bottomExecution();
		} else {
			// restrict each half to the executions it describes
			AnalysisState<A> okState = analysis.assume(state,
					new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, or,
							LogicalNegation.INSTANCE, getLocation()),
					this, this);
			AnalysisState<A> koState = analysis.assume(state, or, this, this);

			Type accessType = arrayType.getInnerType();
			accessType = accessType.isArrayType() ? accessType.asArrayType().getInnerType() : accessType;
			AnalysisState<A> noExceptionState = readArrayElement(analysis, okState, container, right, lenAccess,
					accessType);

			// builds the exception
			JavaClassType oobExc = JavaClassType.getArrayIndexOutOfBoundsExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					oobExc.getReference(), new Expression[0]);
			koState = call.forwardSemanticsAux(interprocedural, koState, new ExpressionSet[0],
					new StatementStore<A>(koState));

			AnalysisState<A> erroneousState = koState.bottomExecution();

			for (SymbolicExpression th : koState.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), oobExc.getReference(), getLocation());
				AnalysisState<A> tmp = analysis.assign(koState, throwVar, th, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				erroneousState = erroneousState.lub(tmp.withExecutionExpression(throwVar));
			}

			return analysis.moveExecutionToError(noExceptionState, erroneousState,
					new Error(oobExc.getReference(), this), this);
		}
	}

	// array elements are written with constant indices (e.g. by reflection
	// metadata like Class::getMethods), so a heap identifier built from a
	// non-constant right (e.g. a loop variable abstracted to top) would be
	// disconnected from every written cell; fall back to joining over all
	// cells within the known bounds when right isn't resolvable to constants
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> readArrayElement(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression container,
			SymbolicExpression right,
			AccessChild lenAccess,
			Type accessType)
			throws SemanticException {
		List<SymbolicExpression> concreteIndexes = new ArrayList<>();
		for (SymbolicExpression rewritten : analysis.rewrite(state, right, this))
			if (rewritten instanceof Constant)
				concreteIndexes.add(rewritten);

		if (!concreteIndexes.isEmpty()) {
			AnalysisState<A> result = state.bottomExecution();
			boolean untracked = false;
			for (SymbolicExpression idx : concreteIndexes) {
				result = result.lub(analysis.smallStepSemantics(state,
						new AccessChild(accessType, container, idx, getLocation()), this));
				Object value = ((Constant) idx).getValue();
				boolean tracked = value instanceof Number && ((Number) value).longValue() < MAX_CONST_ARRAY_SIZE;
				if (!tracked)
					untracked = true;
			}
			// a write with an unknown index might have changed the cells after
			// the first MAX_CONST_ARRAY_SIZE (see below)
			if (untracked && !isWritten())
				result = result.lub(unknownElement(analysis, state, accessType));
			return result;
		}

		AnalysisState<A> result = state.bottomExecution();
		for (int i = 0; i < MAX_CONST_ARRAY_SIZE; i++) {
			Constant idx = new Constant(JavaIntType.INSTANCE, i, getLocation());
			it.unive.lisa.symbolic.value.BinaryExpression inBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE, idx, lenAccess, ComparisonLt.INSTANCE, getLocation());
			if (analysis.satisfies(state, inBounds, this) == Satisfiability.NOT_SATISFIED)
				return result;
			result = result.lub(analysis.smallStepSemantics(state,
					new AccessChild(accessType, container, idx, getLocation()), this));
		}
		// the length might be larger (e.g. it has no upper bound): the cells
		// after the first MAX_CONST_ARRAY_SIZE are not tracked, a read gives
		// any value of the element type, and a write changes only the first
		// cells
		if (isWritten())
			return result;
		return result.lub(unknownElement(analysis, state, accessType));
	}

	// any value of the element type: for references, also null (the cells
	// of a new array are null)
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> unknownElement(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			Type accessType)
			throws SemanticException {
		AnalysisState<A> any = analysis.smallStepSemantics(state, new PushAny(accessType, getLocation()), this);
		if (accessType.isReferenceType() || accessType.isInMemoryType())
			any = any.lub(analysis.smallStepSemantics(state, new NullConstant(getLocation()), this));
		return any;
	}

	// whether this access is the target of a write (an assignment, a compound
	// assignment, an increment or a decrement), which needs the cells
	// TODO: a compound assignment, an increment and a decrement also read the
	// cells, and their value (e.g. x = a[i]++) misses the unknown value of the
	// untracked cells
	private boolean isWritten() {
		Statement parent = getParentStatement();
		if (parent instanceof Assignment || parent instanceof JavaCompoundAssignment)
			return ((BinaryExpression) parent).getLeft() == this;
		return parent instanceof PostfixAddition || parent instanceof PostfixSubtraction
				|| parent instanceof PrefixAddition || parent instanceof PrefixSubtraction;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return getLeft() + "[" + getRight() + "]";
	}
}
