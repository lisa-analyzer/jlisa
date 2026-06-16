package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.jlisa.program.operator.JavaClassGetFieldOperator;
import it.unive.jlisa.program.operator.JavaIsFieldDefinedOperator;
import it.unive.jlisa.program.operator.JavaFieldSetClassNameOperator;
import it.unive.jlisa.program.operator.JavaFieldSetNameOperator;
import it.unive.jlisa.program.operator.JavaFieldSetTypeOperator;
import it.unive.jlisa.program.operator.JavaFieldSetIsInstanceOperator;
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
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
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

		Type booleanType = getProgram().getTypes().getBooleanType();
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

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		if (sat != Satisfiability.NOT_SATISFIED) {

			// allocate the Field object
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(fieldMetaType),
					new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			AnalysisState<A> tmp = callState.bottomExecution();

			// assign class name
			it.unive.lisa.symbolic.value.UnaryExpression fieldSetClassName = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					accessClassNameExpr,
					JavaFieldSetClassNameOperator.INSTANCE,
					getLocation());

			tmp = assignFieldWithOperator(interprocedural, callState, expressions, "className", fieldSetClassName);

			// assign field name
			it.unive.lisa.symbolic.value.UnaryExpression fieldSetName = new it.unive.lisa.symbolic.value.UnaryExpression(
					stringType,
					accessFieldNameExpr,
					JavaFieldSetNameOperator.INSTANCE,
					getLocation());

			tmp = assignFieldWithOperator(interprocedural, tmp, expressions, "name", fieldSetName);

			// assign field type
			it.unive.lisa.symbolic.value.BinaryExpression fieldSetType = new it.unive.lisa.symbolic.value.BinaryExpression(
					stringType,
					accessClassNameExpr,
					accessFieldNameExpr,
					JavaFieldSetTypeOperator.INSTANCE,
					getLocation());
			tmp = assignFieldWithOperator(interprocedural, tmp, expressions, "type", fieldSetType);

			// assign field isInstance
			it.unive.lisa.symbolic.value.BinaryExpression fieldSetIsInstance = new it.unive.lisa.symbolic.value.BinaryExpression(
					booleanType,
					accessClassNameExpr,
					accessFieldNameExpr,
					JavaFieldSetIsInstanceOperator.INSTANCE,
					getLocation());
			tmp = assignFieldWithOperator(interprocedural, tmp, expressions, "isInstance", fieldSetIsInstance);

			getMetaVariables().addAll(call.getMetaVariables());
			noExceptionState = tmp.withExecutionExpressions(callState.getExecutionExpressions());
		}

		if (sat != Satisfiability.SATISFIED) {

			JavaClassType noSuchFieldType = JavaClassType.getNoSuchFieldException();

			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					noSuchFieldType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), noSuchFieldType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
			state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

			exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(noSuchFieldType.getReference(), originating), this);
		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> assignFieldWithOperator(
		InterproceduralAnalysis<A, D> interprocedural,
		AnalysisState<A> state,
		StatementStore<A> expressions,
		String fieldToAssign,
		ValueExpression expr
	) throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type fieldMetaType = JavaClassType.getFieldMetaType();

		GlobalVariable acc = new GlobalVariable(Untyped.INSTANCE, fieldToAssign, getLocation());

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : state.getExecutionExpressions()) {
			AccessChild dst = new AccessChild(fieldMetaType, ref, acc, getLocation());
			AnalysisState<A> sem = analysis.assign(state, dst, expr, this);
			tmp = tmp.lub(sem);
		}

		tmp = tmp.withExecutionExpressions(state.getExecutionExpressions());
		return tmp;
	}

}
