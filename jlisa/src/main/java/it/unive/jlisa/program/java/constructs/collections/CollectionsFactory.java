package it.unive.jlisa.program.java.constructs.collections;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * The factories of {@code java.util.Collections} that create collections that
 * cannot be changed: {@code emptyList()}, {@code emptySet()} (no parameters)
 * and {@code singletonList(o)}, {@code singleton(o)} (one element), and the
 * ones creating collections with the given elements, in order.
 */
public abstract class CollectionsFactory extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the collection.
	 */
	private final CodeLocation collectionLocation;

	/**
	 * The location of the allocation of its array.
	 */
	private final CodeLocation elementDataLocation;

	protected CollectionsFactory(
			CFG cfg,
			CodeLocation location,
			String name,
			Expression... params) {
		super(cfg, location, name, params);
		SyntheticCodeLocationManager synGen = new SyntheticCodeLocationManager(
				"collections-" + name + "-" + location.toString());
		collectionLocation = synGen.nextLocation();
		elementDataLocation = synGen.nextLocation();
	}

	/**
	 * Returns the name of the type of the created collection.
	 *
	 * @return the name of the type
	 */
	protected abstract String collectionType();

	/**
	 * Returns how the created collection can be changed (see
	 * {@link ListModel#IMMUTABLE}): the collections of the factories of
	 * {@code Collections} cannot be changed.
	 *
	 * @return the value of the field {@code $fixed} of the collection
	 */
	protected int fixed() {
		return ListModel.IMMUTABLE;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		LibrarySpecificationProvider.importClass(getProgram(), collectionType());
		AnalysisState<A> created = ListModel.create(analysis, context, state,
				JavaClassType.lookup(collectionType()).getReference(), collectionLocation, elementDataLocation,
				fixed(), this);
		if (params.length == 0)
			return created;

		// the elements, in order
		AnalysisState<A> result = created.bottomExecution();
		for (SymbolicExpression collection : created.getExecutionExpressions()) {
			AnalysisState<A> current = created.withExecutionExpression(collection);
			for (int i = 0; i < params.length; i++) {
				AnalysisState<A> next = current.bottomExecution();
				for (SymbolicExpression element : params[i]) {
					AnalysisState<A> boxed = ListModel.box(interprocedural, current, element,
							getSubExpressions()[i], this, expressions);
					for (SymbolicExpression value : boxed.getExecutionExpressions())
						next = next.lub(ListModel.append(analysis, context, boxed, collection, value, this)
								.withExecutionExpression(collection));
				}
				current = next;
			}
			result = result.lub(current);
		}
		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}
