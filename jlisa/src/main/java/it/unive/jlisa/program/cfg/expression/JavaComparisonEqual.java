package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaComparisonEqual extends Equal {

	public JavaComparisonEqual(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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
		boolean bothRef = (left.getStaticType().isReferenceType() || left.getStaticType().isNullType())
				&& (right.getStaticType().isReferenceType() || right.getStaticType().isNullType());
		boolean bothPrimitive = !left.getStaticType().isReferenceType() && !right.getStaticType().isReferenceType();

		if (bothPrimitive || bothRef) {
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		}

		// try unboxing
		AnalysisState<A> res = state.bottomExecution();
		if (left.getStaticType().isReferenceType()) {
			res = eqWithUnbox(analysis, state, left, right, expressions);
		} else {
			res = eqWithUnbox(analysis, state, right, left, expressions);
		}
		return res;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> eqWithUnbox(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression x,
			SymbolicExpression y,
			StatementStore<A> expressions)
			throws SemanticException {

		AnalysisState<A> res = state.bottomExecution();
		Type t = x.getStaticType();

		if (t instanceof JavaReferenceType jrt && jrt.getInnerType() instanceof JavaClassType innerType) {

			assert (JavaClassType.isWrapperType(innerType));
			SymbolicExpression unboxed = unbox(analysis, state, x, innerType, expressions);

			AnalysisState<A> tmp = analysis.smallStepSemantics(state,
					new BinaryExpression(getStaticType(), unboxed, y, ComparisonEq.INSTANCE, getLocation()), this);
			res = res.lub(tmp);
		} else {
			Constant c = new Constant(BoolType.INSTANCE, false, getLocation());
			res = res.lub(analysis.smallStepSemantics(state, c, this));
		}
		return res;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> SymbolicExpression unbox(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression toUnbox,
			JavaClassType t,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation location = getLocation();
		assert (toUnbox.getStaticType().isReferenceType());

		GlobalVariable g = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference deref = new HeapDereference(t, toUnbox, location);
		AccessChild a = new AccessChild(JavaClassType.getUnwrappedType(t), deref, g, location);
		return a;
	}
}
