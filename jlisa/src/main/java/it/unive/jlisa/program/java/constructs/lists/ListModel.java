package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.Set;

/**
 * The fields of the lists in the abstract state: the number of elements
 * ({@code $size}) and the array that holds them ({@code $elementData}), whose
 * cells are the elements in order.
 */
public class ListModel {

	/**
	 * The number of cells of {@code $elementData} that are tracked one by one:
	 * the elements after them are unknown.
	 */
	public static final int MAX_TRACKED_ELEMENTS = 64;

	private ListModel() {
	}

	/**
	 * Returns the access to the field {@code $size} of the given list.
	 *
	 * @param list     the list
	 * @param location the location of the access
	 *
	 * @return the access to the field
	 */
	public static AccessChild size(
			SymbolicExpression list,
			CodeLocation location) {
		return field(list, "$size", JavaIntType.INSTANCE, location);
	}

	/**
	 * Returns the access to the field {@code $elementData} of the given list.
	 *
	 * @param list     the list
	 * @param location the location of the access
	 *
	 * @return the access to the field
	 */
	public static AccessChild elementData(
			SymbolicExpression list,
			CodeLocation location) {
		return field(list, "$elementData", JavaArrayType.OBJECT_ARRAY, location);
	}

	/**
	 * Returns the access to the cell of {@code $elementData} of the given list
	 * that holds the element at the given index.
	 *
	 * @param list     the list
	 * @param index    the index of the element
	 * @param location the location of the access
	 *
	 * @return the access to the cell
	 */
	public static AccessChild element(
			SymbolicExpression list,
			int index,
			CodeLocation location) {
		HeapDereference array = new HeapDereference(JavaArrayType.OBJECT_ARRAY.getInnerType(),
				elementData(list, location), location);
		return new AccessChild(new JavaReferenceType(JavaClassType.getObjectType()), array,
				new Constant(JavaIntType.INSTANCE, index, location), location);
	}

	/**
	 * Returns the comparison between the field {@code $size} of the given list
	 * and the given value.
	 *
	 * @param list       the list
	 * @param comparison the comparison operator
	 * @param value      the value
	 * @param location   the location of the comparison
	 *
	 * @return the comparison
	 */
	public static BinaryExpression compareSize(
			SymbolicExpression list,
			BinaryOperator comparison,
			int value,
			CodeLocation location) {
		return new BinaryExpression(JavaBooleanType.INSTANCE, size(list, location),
				new Constant(JavaIntType.INSTANCE, value, location), comparison, location);
	}

	/**
	 * Returns the access to the field {@code $list} of the given iterator of a
	 * list: the list that it iterates.
	 *
	 * @param iterator the iterator
	 * @param location the location of the access
	 *
	 * @return the access to the field
	 */
	public static AccessChild iteratedList(
			SymbolicExpression iterator,
			CodeLocation location) {
		return iteratorField(iterator, "$list", JavaClassType.lookup("java.util.List").getReference(), location);
	}

	/**
	 * Returns the access to the field {@code $cursor} of the given iterator of
	 * a list: the index of the element that {@code next} returns.
	 *
	 * @param iterator the iterator
	 * @param location the location of the access
	 *
	 * @return the access to the field
	 */
	public static AccessChild cursor(
			SymbolicExpression iterator,
			CodeLocation location) {
		return iteratorField(iterator, "$cursor", JavaIntType.INSTANCE, location);
	}

	/**
	 * Returns whether the fields of the given iterator are in the state, that
	 * is, the iterator was created by {@code iterator} of a modeled list. The
	 * methods of the other iterators behave as calls to methods that are not
	 * modeled.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param iterator the iterator
	 * @param st       the statement that uses the iterator
	 *
	 * @return whether the fields of the iterator are in the state
	 *
	 * @throws SemanticException if the fields cannot be resolved
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isModeledIterator(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression iterator,
			Statement st)
			throws SemanticException {
		return knows(analysis, context, state, cursor(iterator, st.getLocation()), st)
				&& isModeled(analysis, context, state, iteratedList(iterator, st.getLocation()), st);
	}

	/**
	 * Returns whether the fields of the given list are in the state, that is,
	 * the list was created by a modeled constructor. The methods of the other
	 * lists behave as calls to methods that are not modeled.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param list     the list
	 * @param st       the statement that uses the list
	 *
	 * @return whether the fields of the list are in the state
	 *
	 * @throws SemanticException if the fields cannot be resolved
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isModeled(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression list,
			Statement st)
			throws SemanticException {
		return knows(analysis, context, state, size(list, st.getLocation()), st);
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean knows(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression field,
			Statement st)
			throws SemanticException {
		ExpressionSet ids = analysis.rewrite(state, field, context, st);
		if (ids.isEmpty())
			return false;
		for (SymbolicExpression e : ids)
			if (!(e instanceof Identifier id) || !state.knowsIdentifier(id))
				return false;
		return true;
	}

	/**
	 * Returns the state where the given list is empty, with its elements in a
	 * new array.
	 *
	 * @param <A>                 the kind of the abstract state
	 * @param <D>                 the kind of the abstract domain
	 * @param analysis            the analysis
	 * @param context             the current context
	 * @param state               the state
	 * @param list                the list
	 * @param elementDataLocation the location of the allocation of the array
	 * @param st                  the constructor
	 *
	 * @return the state where the list is empty
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> init(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression list,
			CodeLocation elementDataLocation,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		JavaReferenceType arrayType = JavaArrayType.OBJECT_ARRAY;
		MemoryAllocation created = new MemoryAllocation(arrayType.getInnerType(), elementDataLocation, false);
		HeapReference ref = new HeapReference(arrayType, created, location);
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, context, st);
		AnalysisState<A> tmp = analysis.assign(allocated, elementData(list, location), ref, context, st);
		return analysis.assign(tmp, size(list, location), new Constant(JavaIntType.INSTANCE, 0, location),
				context, st);
	}

	/**
	 * Returns the state where the given collection is a copy of the given
	 * source (e.g., {@code new ArrayList<>(c)}): it has its own array, with the
	 * same elements (the same references, as the copy is shallow). If
	 * {@code distinct}, the copy keeps only distinct elements (e.g.,
	 * {@code new HashSet<>(c)}), so its size is between 1 and the one of the
	 * source (0 for an empty source).
	 *
	 * @param <A>                 the kind of the abstract state
	 * @param <D>                 the kind of the abstract domain
	 * @param analysis            the analysis
	 * @param context             the current context
	 * @param state               the state
	 * @param target              the copy
	 * @param source              the source
	 * @param elementDataLocation the location of the allocation of the array
	 * @param distinct            whether the copy keeps only distinct elements
	 * @param st                  the constructor
	 *
	 * @return the state where the collection is a copy of the source
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> copy(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression target,
			SymbolicExpression source,
			CodeLocation elementDataLocation,
			boolean distinct,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		AnalysisState<A> empty = init(analysis, context, state, target, elementDataLocation, st);
		AccessChild size = size(target, location);
		AnalysisState<A> result = empty.bottomExecution();
		for (int k = 0; k <= MAX_TRACKED_ELEMENTS; k++) {
			boolean beyond = k == MAX_TRACKED_ELEMENTS;
			AnalysisState<A> before = analysis.assume(empty,
					compareSize(source, beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location), context,
					st, st);
			if (before.getExecutionState().isBottom())
				continue;
			AnalysisState<A> tmp = before;
			if (!distinct)
				for (int i = 0; i < Math.min(k, MAX_TRACKED_ELEMENTS); i++)
					tmp = analysis.assign(tmp, element(target, i, location), element(source, i, location), context,
							st);
			if (beyond)
				tmp = analysis.assign(tmp, size, size(source, location), context, st);
			else if (!distinct || k == 0)
				tmp = analysis.assign(tmp, size, new Constant(JavaIntType.INSTANCE, k, location), context, st);
			else
				tmp = analysis.assign(tmp, size, between(size, 1, k, location), context, st);
			result = result.lub(tmp);
		}
		return result;
	}

	/**
	 * Returns a value that is unknown, but between the given bounds (that the
	 * domains read as constraints on the given identifier).
	 *
	 * @param target   the identifier that receives the value
	 * @param low      the lower bound
	 * @param high     the upper bound
	 * @param location the location of the value
	 *
	 * @return the value
	 */
	public static PushFromConstraints between(
			SymbolicExpression target,
			int low,
			int high,
			CodeLocation location) {
		Set<BinaryExpression> bounds = new HashSet<>();
		bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE, new Constant(JavaIntType.INSTANCE, low, location),
				target, ComparisonLe.INSTANCE, location));
		bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE, new Constant(JavaIntType.INSTANCE, high, location),
				target, ComparisonGe.INSTANCE, location));
		return new PushFromConstraints(JavaIntType.INSTANCE, location, bounds);
	}

	/**
	 * Returns the state where the given element is appended to the given list.
	 *
	 * @param <A>      the kind of the abstract state
	 * @param <D>      the kind of the abstract domain
	 * @param analysis the analysis
	 * @param context  the current context
	 * @param state    the state
	 * @param list     the list
	 * @param element  the element
	 * @param st       the statement that appends the element
	 *
	 * @return the state where the element is appended
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> append(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression list,
			SymbolicExpression element,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();

		// the element goes in the cell at index size, each cell under the
		// condition that size denotes it (as for the cells of the arrays)
		AnalysisState<A> result = state.bottomExecution();
		for (int i = 0; i < MAX_TRACKED_ELEMENTS; i++) {
			AnalysisState<A> cell = analysis.assume(state, compareSize(list, ComparisonEq.INSTANCE, i, location),
					context, st, st);
			if (cell.getExecutionState().isBottom())
				continue;
			result = result.lub(analysis.assign(cell, element(list, i, location), element, context, st));
		}

		// the elements after the tracked cells are unknown
		BinaryExpression untracked = compareSize(list, ComparisonGe.INSTANCE, MAX_TRACKED_ELEMENTS, location);
		AnalysisState<A> beyond = analysis.assume(state, untracked, context, st, st);
		if (!beyond.getExecutionState().isBottom())
			result = result.lub(beyond);

		AccessChild size = size(list, location);
		BinaryExpression increment = new BinaryExpression(JavaIntType.INSTANCE, size,
				new Constant(JavaIntType.INSTANCE, 1, location), NumericNonOverflowingAdd.INSTANCE, location);
		return analysis.assign(result, size, increment, context, st);
	}

	/**
	 * Returns the state whose expressions are the given value, boxed if it is a
	 * primitive value (as the elements of the lists are objects).
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param value           the value
	 * @param valueExpr       the expression of the value
	 * @param st              the statement that uses the value
	 * @param expressions     the statement store
	 *
	 * @return the state whose expressions are the (boxed) value
	 *
	 * @throws SemanticException if the value cannot be boxed
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> box(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression value,
			Expression valueExpr,
			Expression st,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type type = valueExpr.getStaticType();
		if (!JavaTypeSystem.isPrimitive(type)) {
			Set<Type> types = analysis.getRuntimeTypesOf(state, value, context, st);
			if (types.size() != 1 || !JavaTypeSystem.isPrimitive(types.iterator().next()))
				return analysis.smallStepSemantics(state, value, context, st);
			type = types.iterator().next();
		}

		// the value is already computed: it is given to the constructor of the
		// wrapper, without evaluating its expression again
		JavaReferenceType boxed = new JavaReferenceType(JavaClassType.getWrappedType(type));
		JavaNewObj wrap = new JavaNewObj(st.getCFG(), valueExpr.getLocation(), boxed,
				new Expression[] { valueExpr });
		AnalysisState<A> result = wrap.forwardSemanticsAux(interprocedural, state,
				new ExpressionSet[] { new ExpressionSet(value) }, expressions);
		st.getMetaVariables().addAll(wrap.getMetaVariables());
		return result;
	}

	/**
	 * How a method that is not modeled one by one changes the number of
	 * elements of a list.
	 */
	public enum SizeChange {
		/**
		 * The number of elements does not change.
		 */
		SAME,
		/**
		 * The number of elements does not decrease.
		 */
		GROWS,
		/**
		 * The number of elements does not increase.
		 */
		SHRINKS,
		/**
		 * There are no elements.
		 */
		EMPTIES
	}

	/**
	 * Returns the state where the given list is changed by a method that is not
	 * modeled one by one: the number of elements changes as given, and the
	 * elements are unknown, unless they are kept (e.g. when other elements are
	 * appended).
	 *
	 * @param <A>       the kind of the abstract state
	 * @param <D>       the kind of the abstract domain
	 * @param analysis  the analysis
	 * @param context   the current context
	 * @param state     the state
	 * @param list      the list
	 * @param change    how the number of elements changes
	 * @param keepCells whether the elements in the list are kept
	 * @param st        the statement that changes the list
	 *
	 * @return the state where the list is changed
	 *
	 * @throws SemanticException if the fields cannot be assigned
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> change(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression list,
			SizeChange change,
			boolean keepCells,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		AnalysisState<A> tmp = state;
		if (!keepCells)
			for (int i = 0; i < MAX_TRACKED_ELEMENTS; i++)
				tmp = analysis.assign(tmp, element(list, i, location),
						new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), location), context, st);
		if (change == SizeChange.SAME)
			return tmp;
		if (change == SizeChange.EMPTIES)
			return analysis.assign(tmp, size(list, location), new Constant(JavaIntType.INSTANCE, 0, location),
					context, st);

		// the new size is bounded by the old one, for each value of the old
		// one (as for the cells of the arrays)
		AccessChild size = size(list, location);
		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);
		AnalysisState<A> result = tmp.bottomExecution();
		for (int k = 0; k <= MAX_TRACKED_ELEMENTS; k++) {
			Constant old = new Constant(JavaIntType.INSTANCE, k, location);
			boolean beyond = k == MAX_TRACKED_ELEMENTS;
			AnalysisState<A> before = analysis.assume(tmp,
					compareSize(list, beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location), context,
					st, st);
			if (before.getExecutionState().isBottom())
				continue;
			Set<BinaryExpression> bounds = new HashSet<>();
			if (change == SizeChange.GROWS)
				bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE, old, size, ComparisonLe.INSTANCE, location));
			else {
				bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE, zero, size, ComparisonLe.INSTANCE, location));
				if (!beyond)
					bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE, old, size, ComparisonGe.INSTANCE,
							location));
			}
			result = result.lub(analysis.assign(before, size,
					new PushFromConstraints(JavaIntType.INSTANCE, location, bounds), context, st));
		}
		return result;
	}

	/**
	 * Returns the state where the given statement raises a new exception of the
	 * given class, not yet moved to the errors.
	 *
	 * @param <A>             the kind of the abstract state
	 * @param <D>             the kind of the abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state
	 * @param className       the name of the class of the exception
	 * @param st              the statement that raises the exception
	 * @param subExpressions  the sub-expressions of the statement
	 * @param expressions     the statement store
	 *
	 * @return the state where the exception is raised
	 *
	 * @throws SemanticException if the exception cannot be created
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> raise(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			String className,
			Statement st,
			Expression[] subExpressions,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = st.getLocation();

		LibrarySpecificationProvider.importClass(st.getProgram(), className);
		JavaClassType excType = JavaClassType.lookup(className);
		JavaNewObj call = new JavaNewObj(st.getCFG(), location, excType.getReference(), new Expression[0]);
		AnalysisState<A> created = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				expressions);

		AnalysisState<A> exceptionState = created.bottomExecution();
		for (SymbolicExpression th : created.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(st.getCFG(), excType.getReference(), location);
			AnalysisState<A> tmp = analysis.assign(created, throwVar, th, context, st);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			tmp = tmp.forgetIdentifiers(call.getMetaVariables(), st);
			for (Expression sub : subExpressions)
				tmp = tmp.forgetIdentifiers(sub.getMetaVariables(), st);
			exceptionState = exceptionState.lub(tmp.withExecutionExpression(throwVar));
		}
		return exceptionState;
	}

	private static AccessChild iteratorField(
			SymbolicExpression iterator,
			String name,
			Type type,
			CodeLocation location) {
		HeapDereference deref = new HeapDereference(JavaClassType.lookup("java.util.Iterator"), iterator, location);
		return new AccessChild(type, deref, new GlobalVariable(Untyped.INSTANCE, name, location), location);
	}

	private static AccessChild field(
			SymbolicExpression list,
			String name,
			Type type,
			CodeLocation location) {
		HeapDereference deref = new HeapDereference(JavaClassType.lookup("java.util.Collection"), list, location);
		return new AccessChild(type, deref, new GlobalVariable(Untyped.INSTANCE, name, location), location);
	}
}
