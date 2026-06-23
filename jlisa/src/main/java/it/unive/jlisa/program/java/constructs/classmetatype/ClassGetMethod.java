package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostGetMethodParameterCountOperator;
import it.unive.jlisa.program.operator.JavaIsMethodDefinedOperator;
import it.unive.jlisa.program.type.JavaArrayType;
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
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ClassGetMethod extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public ClassGetMethod(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "getMethod", left, middle, right);
	}

	public static ClassGetMethod build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetMethod(cfg, location, params[0], params[1], params[2]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type classMetaType = JavaClassType.getClassMetaType();
		Type methodType = JavaClassType.getMethodType();
		Type stringType = getProgram().getTypes().getStringType();

		Type contentType = new JavaReferenceType(classMetaType);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// access class name (1st arg)
		GlobalVariable classNameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		HeapDereference derefClassNameExpr = new HeapDereference(classMetaType, left, getLocation());
		AccessChild accessClassNameExpr = new AccessChild(stringType, derefClassNameExpr, classNameVar, getLocation());

		// access method name (2nd arg)
		GlobalVariable methodNameVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefMethodNameExpr = new HeapDereference(stringType, middle, getLocation());
		AccessChild accessMethodNameExpr = new AccessChild(stringType, derefMethodNameExpr, methodNameVar,
				getLocation());

		// Support variable length depending on the amount of parameters.
		// Access the length property of the parameterTypes array
		java.util.List<SymbolicExpression> exprsList = new java.util.ArrayList<>();
		exprsList.add(accessClassNameExpr);
		exprsList.add(accessMethodNameExpr);

		HeapDereference derefArr = new HeapDereference(JavaArrayType.CLASS_ARRAY.getInnerType(), right, getLocation());

		// Get the length of the array via ghost state if available
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", getLocation());
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE,
				new HeapDereference(JavaArrayType.CLASS_ARRAY, right, getLocation()),
				lenProperty, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression ghostLen = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE,
				accessLen,
				GhostGetMethodParameterCountOperator.INSTANCE,
				getLocation());
		analysis.satisfies(state, ghostLen, originating);
		Integer exactParamCount = JavaClassType.getGetMethodParameterCount();

		if (exactParamCount == null) {
			// fallback: preserve the existing max limit
			exactParamCount = 20;
		}

		for (int i = 0; i < exactParamCount; ++i) {

			Constant var = new Constant(JavaIntType.INSTANCE, i, getLocation());
			AccessChild accessArrayAtIndex = new AccessChild(contentType, derefArr, var, getLocation());
			HeapDereference derefArrayAtIndex = new HeapDereference(classMetaType, accessArrayAtIndex, getLocation());

			AccessChild accessClassName = new AccessChild(stringType, derefArrayAtIndex, classNameVar, getLocation());

			// add the class name of each parameter type
			exprsList.add(accessClassName);
		}

		SymbolicExpression[] exprs = exprsList.toArray(new SymbolicExpression[0]);

		it.unive.jlisa.program.operator.NaryExpression isMethodDefined = new it.unive.jlisa.program.operator.NaryExpression(
			getProgram().getTypes().getBooleanType(),
			exprs,
			JavaIsMethodDefinedOperator.INSTANCE,
			getLocation());

		Satisfiability sat = analysis.satisfies(state, isMethodDefined, originating);

		if (sat == Satisfiability.SATISFIED) {

			GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", getLocation());
			GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
			GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "paramTypes", getLocation());
			GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", getLocation());

			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(methodType),
				new Expression[0]);

			AnalysisState<
				A> methodAllocated = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					expressions);

			noExceptionState = methodAllocated.topExecution();

			// Assign the declaring class to the `declaringClass` field of
			// Method
			// GlobalVariable declaringClassField = new
			// GlobalVariable(Untyped.INSTANCE, "value", getLocation());
			//
			//
			// it.unive.jlisa.program.operator.NaryExpression getMethod = new
			// it.unive.jlisa.program.operator.NaryExpression(
			// methodType,
			// exprs,
			// JavaClassGetMethodOperator.INSTANCE,
			// getLocation());
			//
			// AnalysisState<A> tmp2 = state.bottomExecution();
			// for (SymbolicExpression ref :
			// callState.getExecutionExpressions()) {
			//
			// AccessChild dstDecl = new AccessChild(methodType, ref,
			// declaringClassField, getLocation());
			//
			// AnalysisState<A> sem = analysis.assign(callState, dstDecl,
			// getMethod, this);
			// tmp2 = tmp2.lub(sem);
			// }

			// getMetaVariables().addAll(call.getMetaVariables());
			// noExceptionState =
			// tmp2.withExecutionExpressions(callState.getExecutionExpressions());

		} else if (sat == Satisfiability.NOT_SATISFIED) {
			// TODO: exception path

			exceptionState = state.topExecution();
			/*
			 * // NoSuchMethodException to be thrown if class does not exist or
			 * method // not found if (!classExists) { JavaClassType
			 * noSuchMethodExceptionType =
			 * JavaClassType.getNoSuchMethodException(); JavaNewObj call = new
			 * JavaNewObj(getCFG(), getLocation(),
			 * noSuchMethodExceptionType.getReference(), new Expression[0]);
			 * state = call.forwardSemanticsAux(interprocedural, state, new
			 * ExpressionSet[0], expressions); // assign exception to variable
			 * thrower CFGThrow throwVar = new CFGThrow(getCFG(),
			 * noSuchMethodExceptionType.getReference(), getLocation()); state =
			 * analysis.assign(state, throwVar,
			 * state.getExecutionExpressions().elements.stream().findFirst().get
			 * (), this); // deletes the receiver of the constructor // and all
			 * the metavariables from subexpressions state =
			 * state.forgetIdentifiers(call.getMetaVariables(), this);
			 * exceptionState =
			 * analysis.moveExecutionToError(state.withExecutionExpression(
			 * throwVar) , new Error(noSuchMethodExceptionType.getReference(),
			 * originating), this); }
			 */
		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}
