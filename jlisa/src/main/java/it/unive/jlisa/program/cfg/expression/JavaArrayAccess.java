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
import it.unive.lisa.interprocedural.ScopeId;
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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaArrayAccess extends BinaryExpression {

	/**
	 * The maximum number of cells joined when reading an array element with a
	 * non-constant index. The cells after them are not tracked: a read gives
	 * any value of the element type, and a write changes only the first cells.
	 */
	private static final int MAX_ENUMERATED_CELLS = 64;

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
		ScopeId<A> context = interprocedural.getCurrentContext();

		it.unive.lisa.symbolic.value.BinaryExpression idxCheck1 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, new Constant(JavaIntType.INSTANCE, 0, getLocation()), ComparisonLt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck2 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, lenAccess, ComparisonGe.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression or = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				idxCheck1, idxCheck2, LogicalOr.INSTANCE, getLocation());

		Satisfiability sat = analysis.satisfies(state, or, context, this);
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
				AnalysisState<A> tmp = analysis.assign(state, throwVar, th, context, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oonExc.getReference(), this), context, this));
			}

			return exceptionState;
		} else if (sat == Satisfiability.NOT_SATISFIED) {
			Type accessType = arrayType.getInnerType();
			accessType = accessType.isArrayType() ? accessType.asArrayType().getInnerType() : accessType;
			return readArrayElement(analysis, context, state, container, right, lenAccess, accessType);
		} else if (sat == Satisfiability.BOTTOM) {
			return state.bottomExecution();
		} else {
			// restrict each half to the executions it describes
			AnalysisState<A> okState = analysis.assume(state,
					new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, or,
							LogicalNegation.INSTANCE, getLocation()),
					context, this, this);
			AnalysisState<A> koState = analysis.assume(state, or, context, this, this);

			Type accessType = arrayType.getInnerType();
			accessType = accessType.isArrayType() ? accessType.asArrayType().getInnerType() : accessType;
			AnalysisState<
					A> noExceptionState = readArrayElement(analysis, context, okState, container, right, lenAccess,
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
				AnalysisState<A> tmp = analysis.assign(koState, throwVar, th, context, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				erroneousState = erroneousState.lub(tmp.withExecutionExpression(throwVar));
			}

			return analysis.moveExecutionToError(noExceptionState, erroneousState,
					new Error(oobExc.getReference(), this), context, this);
		}
	}

	// array elements are written with constant indices (e.g. by reflection
	// metadata like Class::getMethods), so a heap identifier built from a
	// non-constant right (e.g. a loop variable abstracted to top) would be
	// disconnected from every written cell; fall back to joining over all
	// cells within the known bounds when right isn't resolvable to constants
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> readArrayElement(
			Analysis<A, D> analysis,
			ScopeId<?> context,
			AnalysisState<A> state,
			SymbolicExpression container,
			SymbolicExpression right,
			AccessChild lenAccess,
			Type accessType)
			throws SemanticException {
		List<SymbolicExpression> concreteIndexes = new ArrayList<>();
		for (SymbolicExpression rewritten : analysis.rewrite(state, right, context, this))
			if (rewritten instanceof Constant)
				concreteIndexes.add(rewritten);

		if (!concreteIndexes.isEmpty()) {
			AnalysisState<A> result = state.bottomExecution();
			boolean untracked = false;
			for (SymbolicExpression idx : concreteIndexes) {
				result = result.lub(analysis.smallStepSemantics(state,
						new AccessChild(accessType, container, idx, getLocation()), context, this));
				if (!(((Constant) idx).getValue() instanceof Number n) || n.longValue() >= MAX_ENUMERATED_CELLS)
					untracked = true;
			}
			// a write with a non-constant index might have changed the cells
			// that are not tracked (see below)
			if (untracked && !isWritten())
				result = result.lub(unknownElement(analysis, context, state, accessType));
			return result;
		}

		if (!accessType.isReferenceType() && !accessType.isInMemoryType()) {
			it.unive.lisa.symbolic.value.BinaryExpression larger = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE, new Constant(JavaIntType.INSTANCE, MAX_ENUMERATED_CELLS, getLocation()),
					lenAccess, ComparisonLt.INSTANCE, getLocation());
			Satisfiability untracked = analysis.satisfies(state, larger, context, this);
			if (untracked == Satisfiability.SATISFIED || untracked == Satisfiability.UNKNOWN) {
				// the length may be larger than the tracked cells: a read gives
				// any value (which is also the join of the cells with it), and
				// a write may change any tracked cell, which are forgotten at
				// once instead of being updated one by one. The value goes to
				// the cell named after the index, which no read uses: it is
				// forgotten too, so that a compound assignment, an increment
				// or a decrement reads any value
				if (!isWritten())
					return unknownElement(analysis, context, state, accessType);
				Set<Identifier> cells = new HashSet<>();
				for (int i = 0; i < MAX_ENUMERATED_CELLS; i++)
					addCells(analysis, context, state, new AccessChild(accessType, container,
							new Constant(JavaIntType.INSTANCE, i, getLocation()), getLocation()), cells);
				AccessChild target = new AccessChild(accessType, container, right, getLocation());
				addCells(analysis, context, state, target, cells);
				return analysis.smallStepSemantics(state.forgetIdentifiers(cells, this), target, context, this);
			}
		}

		AnalysisState<A> result = state.bottomExecution();
		for (int i = 0;; i++) {
			if (i == MAX_ENUMERATED_CELLS)
				// the length is unbounded or too large: a read gives any
				// value, and a write changes only the tracked cells
				return isWritten() ? result : result.lub(unknownElement(analysis, context, state, accessType));
			Constant idx = new Constant(JavaIntType.INSTANCE, i, getLocation());
			it.unive.lisa.symbolic.value.BinaryExpression inBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE, idx, lenAccess, ComparisonLt.INSTANCE, getLocation());
			if (analysis.satisfies(state, inBounds, context, this) == Satisfiability.NOT_SATISFIED)
				break;
			result = result.lub(analysis.smallStepSemantics(state,
					new AccessChild(accessType, container, idx, getLocation()), context, this));
		}
		return result;
	}

	// adds the identifiers of the cells the given access names
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> void addCells(
			Analysis<A, D> analysis,
			ScopeId<?> context,
			AnalysisState<A> state,
			AccessChild access,
			Set<Identifier> cells)
			throws SemanticException {
		for (SymbolicExpression cell : analysis.rewrite(state, access, context, this))
			if (cell instanceof Identifier id)
				cells.add(id);
	}

	// any value of the element type: for references, also null (the cells of
	// a new array are null)
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> unknownElement(
			Analysis<A, D> analysis,
			ScopeId<?> context,
			AnalysisState<A> state,
			Type accessType)
			throws SemanticException {
		AnalysisState<A> any = analysis.smallStepSemantics(state, new PushAny(accessType, getLocation()), context,
				this);
		if (accessType.isReferenceType() || accessType.isInMemoryType())
			any = any.lub(analysis.smallStepSemantics(state, new NullConstant(getLocation()), context, this));
		return any;
	}

	// whether this access is the target of a write (an assignment, a compound
	// assignment, an increment or a decrement), which needs the cells
	// TODO: a compound assignment, an increment and a decrement also read the
	// cells, and their value (e.g., x = a[i]++) misses the cells that are not
	// tracked
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
