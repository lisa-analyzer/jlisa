package it.unive.jlisa.program.java.constructs.bitset;

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
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.type.Untyped;

/**
 * {@code BitSet::get}: the bits of a bit set where no bit has been set are all
 * clear; the other bits are unknown.
 */
public class BitSetGet extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public BitSetGet(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", JavaBooleanType.INSTANCE, left, right);
	}

	public static BitSetGet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new BitSetGet(cfg, location, params[0], params[1]);
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

	/**
	 * Yields the field recording whether no bit of the given bit set has been
	 * set.
	 *
	 * @param bitset   the bit set
	 * @param location the location where the field is accessed
	 *
	 * @return the field
	 */
	static AccessChild empty(
			SymbolicExpression bitset,
			CodeLocation location) {
		return new AccessChild(JavaBooleanType.INSTANCE, bitset,
				new GlobalVariable(Untyped.INSTANCE, "$empty", location), location);
	}

	/**
	 * Records whether no bit of the given bit set has been set.
	 *
	 * @param <A>             the kind of lattice
	 * @param <D>             the kind of domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the current state
	 * @param bitset          the bit set
	 * @param empty           whether no bit has been set
	 * @param st              the statement updating the bit set
	 *
	 * @return the updated state
	 *
	 * @throws SemanticException if the semantics fails
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> setEmpty(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression bitset,
			boolean empty,
			Statement st)
			throws SemanticException {
		CodeLocation location = st.getLocation();
		return interprocedural.getAnalysis().assign(state, empty(bitset, location),
				new Constant(JavaBooleanType.INSTANCE, empty, location), interprocedural.getCurrentContext(), st);
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
		CodeLocation location = getLocation();
		// a negative index raises an exception: the bit is known only for
		// non-negative ones
		boolean clear = analysis.satisfies(state, empty(left, location), context, this) == Satisfiability.SATISFIED
				&& analysis.satisfies(state,
						new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE, right,
								new Constant(JavaIntType.INSTANCE, 0, location), ComparisonGe.INSTANCE, location),
						context, this) == Satisfiability.SATISFIED;
		SymbolicExpression bit = clear ? new Constant(JavaBooleanType.INSTANCE, false, location)
				: new PushAny(JavaBooleanType.INSTANCE, location);
		return analysis.smallStepSemantics(state, bit, context, this);
	}
}
