package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostTypeLookupOperator;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ClassGetMethod extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public ClassGetMethod(
			CFG cfg,
			CodeLocation location,
			Expression p0,
			Expression p1,
			Expression p2) {
		super(cfg, location, "getMethod", p0, p1, p2);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		SymbolicExpression[] exprs = new SymbolicExpression[params.length];

		for (int i = 0; i < params.length; ++i) {
			ExpressionSet set = params[i];
			if (set.size() > 1 || set.size() <= 0)
				throw new IllegalArgumentException("Number of operands is incorrect!");
			for (SymbolicExpression expr : set) {
				exprs[i] = expr;
			}
		}

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type classMetaType = JavaClassType.getClassMetaType();
		Type methodType = JavaClassType.getMethodType();
		Type stringType = getProgram().getTypes().getStringType();

		// Extract the dynamic class type from the receiver (Class object)
		GlobalVariable nameField = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		HeapDereference derefExpr = new HeapDereference(classMetaType, exprs[0], getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, nameField, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				GhostTypeLookupOperator.INSTANCE,
				getLocation());

		analysis.satisfies(state, un, originating);
		String dynamicTypeStr = JavaClassType.getDynamicTypeLookup();

		// Check if the class exists
		boolean classExists = true;
		try {
			JavaClassType.lookup(dynamicTypeStr);
		} catch (IllegalArgumentException e) {
			classExists = false;
		}

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// Populate the "no exception" path
		if (classExists) {
			// Allocate the Method object
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(methodType),
					new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// Assign the method name to the `name` field of Method
			GlobalVariable methodNameField = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild dst = new AccessChild(stringType, ref, methodNameField, getLocation());

				// Use PushAny as a top value for the method name
				PushAny top = new PushAny(stringType, getLocation());
				AnalysisState<A> sem = analysis.assign(callState, dst, top, this);
				tmp = tmp.lub(sem);
			}

			// Assign the declaring class to the `declaringClass` field of Method
			GlobalVariable declaringClassField = new GlobalVariable(Untyped.INSTANCE, "declaringClass",
					getLocation());

			AnalysisState<A> tmp2 = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild dstDecl = new AccessChild(classMetaType, ref, declaringClassField, getLocation());
				AnalysisState<A> sem = analysis.assign(tmp, dstDecl, exprs[0], this);
				tmp2 = tmp2.lub(sem);
			}

			getMetaVariables().addAll(call.getMetaVariables());
			noExceptionState = tmp2.withExecutionExpressions(callState.getExecutionExpressions());
		}

		// NoSuchMethodException to be thrown if class does not exist or method
		// not found
		if (!classExists) {
			JavaClassType noSuchMethodExceptionType = JavaClassType.getNoSuchMethodException();

			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					noSuchMethodExceptionType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), noSuchMethodExceptionType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);

			exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(noSuchMethodExceptionType.getReference(), originating), this);
		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}
