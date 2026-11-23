package it.unive.jlisa.program.java.constructs.string;

import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
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
import it.unive.lisa.program.SourceCodeLocation;
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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringSubstring extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringSubstring(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "substring", left, right);
	}

	public static StringSubstring build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringSubstring(cfg, location, params[0], params[1]);
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

		// JLS: begin < 0 or begin > length, exactly where the condition
		// holds, and the substring is computed in the other executions
		it.unive.lisa.symbolic.value.UnaryExpression length = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE, accessLeft, JavaStringLengthOperator.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression beginNegative = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, right, new Constant(JavaIntType.INSTANCE, 0, getLocation()),
				ComparisonLt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression beginTooLarge = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, right, length, ComparisonGt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression outOfRange = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, beginNegative, beginTooLarge, LogicalOr.INSTANCE, getLocation());
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		AnalysisState<A> ok = raising.raiseWhere(outOfRange, true, state, "java.lang.IndexOutOfBoundsException");
		if (ok.getExecutionState().isBottom())
			return raising.moveFrom(ok.bottomExecution());

		it.unive.lisa.symbolic.value.BinaryExpression substring = new it.unive.lisa.symbolic.value.BinaryExpression(
				stringType, accessLeft, right, JavaStringSubstringOperator.INSTANCE, getLocation());

		// allocate the string
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, ok, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = ok.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			tmp = tmp.lub(analysis.assign(callState, access, substring, context, this));
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return raising.moveFrom(tmp.withExecutionExpressions(callState.getExecutionExpressions()));
	}
}
