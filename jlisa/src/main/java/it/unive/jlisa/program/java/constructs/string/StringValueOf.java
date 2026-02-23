package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringValueOf extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringValueOf(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "valueOf", exp);
	}

	public static StringValueOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringValueOf(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression valueOf = null;

		if (expr.getStaticType() instanceof JavaLongType) {
			valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					expr,
					JavaStringValueOfLongOperator.INSTANCE,
					getLocation());
		}

		if (expr.getStaticType() instanceof JavaCharType) {
			valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					expr,
					JavaStringValueOfCharOperator.INSTANCE,
					getLocation());
		}

		if (expr.getStaticType() instanceof JavaFloatType) {
			valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					expr,
					JavaStringValueOfFloatOperator.INSTANCE,
					getLocation());
		}

		if (expr.getStaticType() instanceof JavaIntType) {
			valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					expr,
					JavaStringValueOfIntOperator.INSTANCE,
					getLocation());
		}

		if (expr.getStaticType() instanceof JavaDoubleType) {
			valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					expr,
					JavaStringValueOfDoubleOperator.INSTANCE,
					getLocation());
		}

		// allocate the string
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = interprocedural.getAnalysis().assign(callState, access, valueOf, this);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return tmp.withExecutionExpressions(callState.getExecutionExpressions());

	}
}
