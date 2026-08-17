package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.Satisfiability;
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
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringBuilderEnsureCapacity extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderEnsureCapacity(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "ensureCapacity", left, right);
	}

	public static StringBuilderEnsureCapacity build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderEnsureCapacity(cfg, location, params[0], params[1]);
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

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable capVar = new GlobalVariable(Untyped.INSTANCE, "capacity", getLocation());
		HeapDereference derefLeft = new HeapDereference(stringType, left, getLocation());
		AccessChild capacityRead = new AccessChild(JavaIntType.INSTANCE, derefLeft, capVar, getLocation());
		AccessChild capacityWrite = new AccessChild(JavaIntType.INSTANCE, left, capVar, getLocation());

		// newCapacity = max(minCapacity, capacity * 2 + 2), as per the Java
		// spec
		it.unive.lisa.symbolic.value.BinaryExpression doubled = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaIntType.INSTANCE, capacityRead, new Constant(JavaIntType.INSTANCE, 2, getLocation()),
				NumericNonOverflowingMul.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression doubledPlus2 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaIntType.INSTANCE, doubled, new Constant(JavaIntType.INSTANCE, 2, getLocation()),
				NumericNonOverflowingAdd.INSTANCE, getLocation());

		it.unive.lisa.symbolic.value.BinaryExpression growNeeded = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, right, capacityRead, ComparisonGt.INSTANCE, getLocation());
		Satisfiability sat = analysis.satisfies(state, growNeeded, this);

		AnalysisState<A> result = state.bottomExecution();
		if (sat != Satisfiability.NOT_SATISFIED)
			result = result.lub(assignMax(analysis, state, capacityWrite, right, doubledPlus2));
		if (sat != Satisfiability.SATISFIED)
			// capacity is already sufficient: no-op
			result = result.lub(state);

		return result.withExecutionExpression(new Skip(getLocation()));
	}

	// assigns target := a > b ? a : b
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> assignMax(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression target,
			SymbolicExpression a,
			SymbolicExpression b)
			throws SemanticException {
		it.unive.lisa.symbolic.value.BinaryExpression aGtB = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, a, b, ComparisonGt.INSTANCE, getLocation());
		Satisfiability sat = analysis.satisfies(state, aGtB, this);

		AnalysisState<A> result = state.bottomExecution();
		if (sat != Satisfiability.NOT_SATISFIED)
			result = result.lub(analysis.assign(state, target, a, this));
		if (sat != Satisfiability.SATISFIED)
			result = result.lub(analysis.assign(state, target, b, this));
		return result;
	}
}