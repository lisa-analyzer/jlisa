package it.unive.jlisa.program.java.constructs.string;

import java.util.Set;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfObjectOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringValueOfObject extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringValueOfObject(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "valueOf", exp);
	}

	public static StringValueOfObject build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringValueOfObject(cfg, location, params[0]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, context, this);
		AnalysisState<A> res = state.bottomExecution();

		for (Type t : types) {

			assert(t instanceof JavaReferenceType);
			Type innerType = t.asReferenceType().getInnerType();
			if (innerType.equals(JavaClassType.getStringType())) {
				res = res.lub(analysis.smallStepSemantics(state, expr, context, this));
				continue;
			}
			// TODO: handle null case, if literal it's nullpointerexc, if not "null" string is returned

			Type unwrappedType = JavaClassType.getUnwrappedType(innerType);
			HeapDereference deref = new HeapDereference(innerType, expr, getLocation());
			AccessChild accessExpr = new AccessChild(unwrappedType, deref, var, getLocation());

			UnaryOperator op = JavaStringValueOfObjectOperator.INSTANCE;
			if (unwrappedType instanceof JavaLongType) {
				op = JavaStringValueOfLongOperator.INSTANCE;
			}
			else if (unwrappedType instanceof JavaCharType) {
				op = JavaStringValueOfCharOperator.INSTANCE;
			}
			else if (unwrappedType instanceof JavaFloatType) {
				op = JavaStringValueOfFloatOperator.INSTANCE;
			}
			else if (unwrappedType instanceof JavaIntType) {
				op = JavaStringValueOfIntOperator.INSTANCE;
			}
			else if (unwrappedType instanceof JavaDoubleType) {
				op = JavaStringValueOfDoubleOperator.INSTANCE;
			}
			else if (unwrappedType instanceof JavaBooleanType) {
				op = JavaStringValueOfBooleanOperator.INSTANCE;
			}

			// TODO: short (not needed for svcomp)

			it.unive.lisa.symbolic.value.UnaryExpression valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					accessExpr,
					op,
					getLocation());

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
			res = res.lub(tmp.withExecutionExpressions(callState.getExecutionExpressions()));
		}
		return res;
	}
}
