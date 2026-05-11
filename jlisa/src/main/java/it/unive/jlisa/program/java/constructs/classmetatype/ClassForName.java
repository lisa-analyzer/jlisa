package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaClassForNameOperator;
import it.unive.jlisa.program.operator.JavaIsClassDefinedOperator;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ClassForName extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public ClassForName(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "forName", JavaClassType.getClassMetaType(), expr);
	}

	public static ClassForName build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassForName(cfg, location, params[0]);
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

		Type stringType = getProgram().getTypes().getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefExpr = new HeapDereference(stringType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, getLocation());

		// check if class actually exists
		it.unive.lisa.symbolic.value.UnaryExpression isClassInProgram = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				JavaIsClassDefinedOperator.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, isClassInProgram, originating);

		// we are sure the class exists
		if (sat == Satisfiability.SATISFIED) {

			it.unive.lisa.symbolic.value.UnaryExpression forName = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					accessExpr,
					JavaClassForNameOperator.INSTANCE,
					getLocation());

			// allocate the Class object
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(classMetaType),
					new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// `name` field of Class type
			GlobalVariable nameField = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild dst = new AccessChild(stringType, ref, nameField, getLocation());
				AnalysisState<A> sem = analysis.assign(callState, dst, forName, this);
				tmp = tmp.lub(sem);
			}

			getMetaVariables().addAll(call.getMetaVariables());
			return tmp.withExecutionExpressions(callState.getExecutionExpressions());
		}

		if (sat == Satisfiability.NOT_SATISFIED) {
			// TODO
		}

		return state;

	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}
