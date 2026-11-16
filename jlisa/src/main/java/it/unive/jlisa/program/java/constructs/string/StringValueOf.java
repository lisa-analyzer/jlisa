package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
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
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

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

		UnaryOperator operator = operator(expr.getStaticType());
		if (operator == null) {
			// e.g., the length of an array, or one of its elements: the
			// operator is the one of the runtime type of the value
			Set<Type> types = interprocedural.getAnalysis().getRuntimeTypesOf(state, expr,
					interprocedural.getCurrentContext(), this);
			if (types.size() == 1)
				operator = operator(types.iterator().next());
		}
		// if the type of the value is not known, the string is unknown
		SymbolicExpression valueOf = operator == null ? new PushAny(stringType, getLocation())
				: new it.unive.lisa.symbolic.value.UnaryExpression(stringType, expr, operator, getLocation());

		// allocate the string
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = interprocedural.getAnalysis().assign(callState, access, valueOf,
					interprocedural.getCurrentContext(), this);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return tmp.withExecutionExpressions(callState.getExecutionExpressions());

	}

	// the operator converting a value of the given primitive type to a string,
	// or null
	private static UnaryOperator operator(
			Type type) {
		if (type instanceof JavaLongType)
			return JavaStringValueOfLongOperator.INSTANCE;
		if (type instanceof JavaCharType)
			return JavaStringValueOfCharOperator.INSTANCE;
		if (type instanceof JavaFloatType)
			return JavaStringValueOfFloatOperator.INSTANCE;
		if (type instanceof JavaDoubleType)
			return JavaStringValueOfDoubleOperator.INSTANCE;
		if (type instanceof JavaBooleanType)
			return JavaStringValueOfBooleanOperator.INSTANCE;
		// bytes and shorts are written as ints
		if (type instanceof JavaIntType || type instanceof JavaByteType || type instanceof JavaShortType)
			return JavaStringValueOfIntOperator.INSTANCE;
		return null;
	}
}
