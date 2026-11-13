package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringBuilderGetChars extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderGetChars(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, "getChars", params);
	}

	public static StringBuilderGetChars build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderGetChars(cfg, location, params);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();

		SymbolicExpression[] actualParams = new SymbolicExpression[5];

		for (int i = 0; i < 5; ++i) {
			ExpressionSet e = params[i];
			if (e.size() > 1)
				return state;
			actualParams[i] = e.iterator().next();
		}

		JavaReferenceType charArrType = JavaArrayType.CHAR_ARRAY;

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference deref = new HeapDereference(JavaClassType.lookup("java.lang.StringBuilder"), actualParams[0], getLocation());
		AccessChild accessExpr = new AccessChild(stringType, deref, var, getLocation());

		HeapDereference derefDst = new HeapDereference(charArrType.getInnerType(), actualParams[3], location);

		if (!(actualParams[1] instanceof Constant)) {
			return state;
		}
		if (!(actualParams[4] instanceof Constant)) {
			return state;
		}

		Constant srcBegin = (Constant) actualParams[1];
		Constant dstBegin = (Constant) actualParams[4];
		SymbolicExpression srcEnd = actualParams[2];

		AnalysisState<A> result = state;

		boolean mayBeOutOfBounds = false;
		while (!mayBeOutOfBounds) {

			it.unive.lisa.symbolic.value.BinaryExpression ltLen = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE,
					srcBegin, srcEnd, ComparisonLt.INSTANCE, location);

			Satisfiability withinBounds = analysis.satisfies(result, ltLen, context, this);
			if (withinBounds != Satisfiability.SATISFIED)
				break;

			it.unive.lisa.symbolic.value.BinaryExpression charAt = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaCharType.INSTANCE,
					accessExpr, srcBegin, JavaStringCharAtOperator.INSTANCE, location);

			AccessChild accessIdx = new AccessChild(JavaCharType.INSTANCE, derefDst, dstBegin, location);
			result = analysis.assign(result, accessIdx, charAt, context, this);

			int v1 = (int) srcBegin.getValue() + 1;
			int v2 = (int) dstBegin.getValue() + 1;

			srcBegin = new Constant(JavaIntType.INSTANCE, v1, location);
			dstBegin = new Constant(JavaIntType.INSTANCE, v2, location);
		}

		return result;
	}

}
