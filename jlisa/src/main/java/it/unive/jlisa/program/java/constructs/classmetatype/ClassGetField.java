package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaClassGetFieldOperator;
import it.unive.jlisa.program.operator.JavaIsFieldDefinedOperator;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ClassGetField extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected ClassGetField(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "getField", left, right);
	}

	public static ClassGetField build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetField(cfg, location, params[0], params[1]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type stringType = getProgram().getTypes().getStringType();
		Type fieldMetaType = JavaClassType.getFieldMetaType();

		// access class name (1st arg)
		GlobalVariable classNameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		HeapDereference derefClassNameExpr = new HeapDereference(stringType, left, getLocation());
		AccessChild accessClassNameExpr = new AccessChild(stringType, derefClassNameExpr, classNameVar, getLocation());

		// access field name (2nd arg)
		GlobalVariable fieldNameVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefFieldNameExpr = new HeapDereference(stringType, right, getLocation());
		AccessChild accessFieldNameExpr = new AccessChild(stringType, derefFieldNameExpr, fieldNameVar, getLocation());

		// check if field actually exists in the given class
		it.unive.lisa.symbolic.value.BinaryExpression isFieldDefined = new it.unive.lisa.symbolic.value.BinaryExpression(
				stringType,
				accessClassNameExpr,
				accessFieldNameExpr,
				JavaIsFieldDefinedOperator.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, isFieldDefined, originating);

		if (sat != Satisfiability.NOT_SATISFIED) {

			// allocate the Field object
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(fieldMetaType),
					new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			it.unive.lisa.symbolic.value.BinaryExpression getField = new it.unive.lisa.symbolic.value.BinaryExpression(
					fieldMetaType,
					accessClassNameExpr,
					accessFieldNameExpr,
					JavaClassGetFieldOperator.INSTANCE,
					getLocation());

			GlobalVariable valueField = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild dst = new AccessChild(fieldMetaType, ref, valueField, getLocation());
				AnalysisState<A> sem = analysis.assign(callState, dst, getField, this);
				tmp = tmp.lub(sem);
			}

			getMetaVariables().addAll(call.getMetaVariables());
			return tmp.withExecutionExpressions(callState.getExecutionExpressions());
		}

		// TODO: `NoSuchFieldException`
		if (sat != Satisfiability.SATISFIED) {
		}

		return state;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}
