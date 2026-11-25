package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringCharAt extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringCharAt(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "charAt", left, right);
	}

	public static StringCharAt build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringCharAt(cfg, location, params[0], params[1]);
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
		ScopeId<A> context = interprocedural.getCurrentContext();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(stringType, left, getLocation());
		AccessChild accessLeft = new AccessChild(stringType, derefLeft, var, getLocation());

		// check for StringIndexOutOfBoundsException
		// index < 0 or index >= length
		it.unive.lisa.symbolic.value.UnaryExpression length = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE,
				accessLeft,
				JavaStringLengthOperator.INSTANCE,
				getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck1 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, new Constant(JavaIntType.INSTANCE, 0, getLocation()), ComparisonLt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck2 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, length, ComparisonGe.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression or = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				idxCheck1, idxCheck2, LogicalOr.INSTANCE, getLocation());

		// StringIndexOutOfBoundsException exactly where the condition holds,
		// the operation is performed in the other executions
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		AnalysisState<A> ok = raising.raiseWhere(or, true, state, "java.lang.StringIndexOutOfBoundsException");
		if (ok.getExecutionState().isBottom())
			return raising.moveFrom(ok.bottomExecution());
		it.unive.lisa.symbolic.value.BinaryExpression concat = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaCharType.INSTANCE,
				accessLeft,
				right,
				JavaStringCharAtOperator.INSTANCE,
				getLocation());

		return raising.moveFrom(analysis.smallStepSemantics(ok, concat, context, originating));
	}
}
