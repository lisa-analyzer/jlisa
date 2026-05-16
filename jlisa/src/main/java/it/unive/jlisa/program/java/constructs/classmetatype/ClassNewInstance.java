package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostTypeLookupOperator;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState.Error;
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
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ClassNewInstance extends it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ClassNewInstance(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "newInstance", JavaClassType.getClassMetaType(), expr);
	}

	public static ClassNewInstance build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassNewInstance(cfg, location, params[0]);
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

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());
		HeapDereference derefExpr = new HeapDereference(classMetaType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				GhostTypeLookupOperator.INSTANCE,
				getLocation());

		// weird workaround to get the dynamic type out of Class->name
		analysis.satisfies(state, un, originating);
		String dynamicTypeStr = JavaClassType.getDynamicTypeLookup();

		// if this fails, throw a `InstantiationException`, meaning that
		// the class is an abstract class, or interface etc...
		boolean canInstantiate = true;
		Type dynamicType = null;
		try {
			dynamicType = JavaClassType.lookup(dynamicTypeStr);
		}
		catch (IllegalArgumentException e) {
			canInstantiate = false;
		}

		// exception path for `InstantiationException`
		if (!canInstantiate) {
			JavaClassType instantiationExceptionType = JavaClassType.getInstantiationException();

			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					instantiationExceptionType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), instantiationExceptionType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

			AnalysisState<A> exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(instantiationExceptionType.getReference(), originating), this);

			return exceptionState;
		}

		// TODO: IllegalAccessException

		// allocate the new object
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(dynamicType),
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		getMetaVariables().addAll(call.getMetaVariables());

		return callState;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}
