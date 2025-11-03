package it.unive.jlisa.program.java.constructs.math;

import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

public class MathMax extends it.unive.lisa.program.cfg.statement.BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public MathMax(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "max", left, right);
	}

	public static MathMax build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MathMax(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
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
		Set<Type> leftTypes = analysis.getRuntimeTypesOf(state, left, originating);
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, originating);

		AnalysisState<A> result = state.bottomExecution();

		for (Type lType : leftTypes) {
			for (Type rType : rightTypes) {
				Type lPrimeType;
				SymbolicExpression leftExpr = left;
				if (lType.isReferenceType() && (lPrimeType = JavaClassType.isWrapperClass(lType.asReferenceType().getInnerType())) != null) {
					// unboxing
					GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
					HeapDereference derefLeft = new HeapDereference(lPrimeType, left, getLocation());
					leftExpr = new AccessChild(lPrimeType, derefLeft, var, getLocation());
				}

				Type rPrimeType;
				SymbolicExpression rightExpr = left;
				if (rType.isReferenceType() && (rPrimeType = JavaClassType.isWrapperClass(rType.asReferenceType().getInnerType())) != null) {
					// unboxing
					GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
					HeapDereference derefRight = new HeapDereference(rPrimeType, right, getLocation());
					rightExpr = new AccessChild(rPrimeType, derefRight, var, getLocation());
				}

				BinaryExpression max = new BinaryExpression(
						JavaDoubleType.INSTANCE,
						leftExpr,
						rightExpr,
						JavaMathMax.INSTANCE,
						getLocation());
				result = result.lub(analysis.smallStepSemantics(state, max, originating));
			}
		}

		return result;
	}
}
