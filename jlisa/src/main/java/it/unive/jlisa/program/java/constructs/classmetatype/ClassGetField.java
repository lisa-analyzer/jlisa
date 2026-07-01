package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostGetMethodParameterCountOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
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
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
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
		CodeLocation location = getLocation();

		Type stringType = getProgram().getTypes().getStringType();
		Type refStringType = new JavaReferenceType(stringType);
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type refFieldMetaType = new JavaReferenceType(fieldMetaType);
		Type fieldArr = JavaArrayType.lookup(refFieldMetaType, 1);
		Type classMetaType = JavaClassType.getClassMetaType();

		// access class name (1st arg)
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);

		// access field name (2nd arg)
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference derefFieldNameExpr = new HeapDereference(stringType, right, location);
		AccessChild accessFieldNameExpr = new AccessChild(stringType, derefFieldNameExpr, valueVar, location);

		GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);


		// get number of fields
		HeapDereference derefClazz = new HeapDereference(classMetaType, left, location);
		AccessChild accessClazzFields = new AccessChild(new JavaReferenceType(fieldArr), derefClazz, declaredFieldsVar, location);

		HeapDereference derefArr = new HeapDereference(fieldArr, accessClazzFields, location);

		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		// FIXME AP: instead of this, use the same thing that's used in getMethod.
		// Check whether the index is within bounds at each iteration.
		it.unive.lisa.symbolic.value.UnaryExpression ghostLen = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE,
				accessLen,
				GhostGetMethodParameterCountOperator.INSTANCE,
				location);
		analysis.satisfies(state, ghostLen, originating);
		Integer arrLen = JavaClassType.getGetMethodParameterCount();

		Satisfiability sat = Satisfiability.NOT_SATISFIED;

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// look for a field with the same name
		for (int i = 0; i < arrLen; ++i) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			AccessChild accessIdx = new AccessChild(refFieldMetaType, derefArr, idx, getLocation());
			HeapDereference derefField = new HeapDereference(fieldMetaType, accessIdx, location);

			AccessChild accessName = new AccessChild(refStringType, derefField, nameVar, location);
			HeapDereference derefName = new HeapDereference(stringType, accessName, location);
			AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

			it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
					getProgram().getTypes().getBooleanType(),
					accessValue,
					accessFieldNameExpr,
					JavaStringEqualsOperator.INSTANCE,
					getLocation());

			Satisfiability fieldFound = analysis.satisfies(state, equalsExpr, originating);

			if (fieldFound == Satisfiability.SATISFIED) {
				sat = Satisfiability.SATISFIED;
				HeapReference refField = new HeapReference(refFieldMetaType, accessIdx, getLocation());
				noExceptionState = analysis.smallStepSemantics(state, refField, originating);
				break;
			}
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

}
