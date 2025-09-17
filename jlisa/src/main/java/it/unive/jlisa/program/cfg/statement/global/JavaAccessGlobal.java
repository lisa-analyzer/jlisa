package it.unive.jlisa.program.cfg.statement.global;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.ConstantGlobal;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An access to a {@link Global} of a {@link Unit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaAccessGlobal extends Expression {

	/**
	 * The receiver of the access
	 */
	private final Unit container;

	/**
	 * The global being accessed
	 */
	private final Global target;

	/**
	 * Builds the global access, happening at the given location in the program.
	 * The type of this expression is the one of the accessed global.
	 * 
	 * @param cfg       the cfg that this expression belongs to
	 * @param location  the location where the expression is defined within the
	 *                      program
	 * @param container the unit containing the accessed global
	 * @param target    the accessed global
	 */
	public JavaAccessGlobal(
			CFG cfg,
			CodeLocation location,
			Unit container,
			Global target) {
		super(cfg, location, target.getStaticType());
		this.container = container;
		this.target = target;
	}

	/**
	 * Yields the {@link Unit} where the global targeted by this access is
	 * defined.
	 * 
	 * @return the container of the global
	 */
	public Unit getContainer() {
		return container;
	}

	/**
	 * Yields the {@link Global} targeted by this expression.
	 * 
	 * @return the global
	 */
	public Global getTarget() {
		return target;
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaAccessGlobal other = (JavaAccessGlobal) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	protected int compareSameClass(
			Statement o) {
		JavaAccessGlobal other = (JavaAccessGlobal) o;
		int cmp;
		if ((cmp = container.getName().compareTo(other.container.getName())) != 0)
			return cmp;
		return target.getName().compareTo(other.target.getName());
	}

	@Override
	public String toString() {
		return container.getName() + "::" + target.getName();
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		if (state.getExecutionInfo(InitializedClassSet.INFO_KEY) == null)
			state = state.storeExecutionInfo(InitializedClassSet.INFO_KEY, new InitializedClassSet());

		// we need to check whether to call the clinit of the container unit or
		// to call the one of its superclass
		Unit classInit = container;
		if (((ClassUnit) container).getGlobal(target.getName()) == null) {
			Set<it.unive.lisa.program.CompilationUnit> superClasses = ((ClassUnit) container)
					.getImmediateAncestors().stream()
					.filter(u -> u instanceof ClassUnit)
					.collect(Collectors.toSet());

			// we can safely suppose that there exist a single superclass
			classInit = (ClassUnit) superClasses.stream().findFirst().get();
		}

		// if needed, calling the class initializer
		if (!JavaClassType.lookup(classInit.toString(), null).getUnit()
				.getCodeMembersByName(classInit.toString() + InitializedClassSet.SUFFIX_CLINIT).isEmpty())
			if (!state.getExecutionInfo(InitializedClassSet.INFO_KEY, InitializedClassSet.class)
					.contains(classInit.toString())) {
				UnresolvedCall clinit = new UnresolvedCall(
						getCFG(),
						getLocation(),
						CallType.STATIC,
						classInit.toString(),
						classInit.toString() + InitializedClassSet.SUFFIX_CLINIT,
						new Expression[0]);

				state = state.storeExecutionInfo(InitializedClassSet.INFO_KEY,
						state.getExecutionInfo(InitializedClassSet.INFO_KEY, InitializedClassSet.class)
								.add(classInit.toString()));
				state = clinit.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
			}

		if (target instanceof ConstantGlobal)
			return analysis.smallStepSemantics(state, ((ConstantGlobal) target).getConstant(), this);

		// unit globals are unique, we can directly access those

		return analysis.smallStepSemantics(
				state,
				new GlobalVariable(target.getStaticType(), classInit.getName() + "::" + target.getName(),
						target.getAnnotations(), getLocation()),
				this);
	}
}