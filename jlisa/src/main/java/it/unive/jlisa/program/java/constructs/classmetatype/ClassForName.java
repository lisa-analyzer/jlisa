package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.GhostTypeLookupOperator;
import it.unive.jlisa.program.operator.JavaClassForNameOperator;
import it.unive.jlisa.program.operator.JavaIsClassDefinedOperator;
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
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
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
import java.util.Set;
import java.util.stream.Collectors;

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

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// populate the "no exception" path
		if (sat != Satisfiability.NOT_SATISFIED) {

			String loadingClassStr = getDynamicClassType(interprocedural, state, expr);
			JavaClassType loadingClass = JavaClassType.lookup(loadingClassStr);

			// execute static initializer
			ClassUnit classUnit = (ClassUnit) loadingClass.getUnit();
			if (classUnit.getCodeMembersByName(loadingClassStr).isEmpty()) {
				Set<CompilationUnit> superClasses = classUnit
						.getImmediateAncestors().stream()
						.filter(u -> u instanceof ClassUnit)
						.collect(Collectors.toSet());

				classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
			}
			state = InitializedClassSet.initialize(state, loadingClass.getReference(), this,
					interprocedural);

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
			noExceptionState = tmp.withExecutionExpressions(callState.getExecutionExpressions());
		}

		// `ClassNotFoundException to be thrown
		if (sat != Satisfiability.SATISFIED) {

			JavaClassType classNotFoundType = JavaClassType.getClassNotFoundException();

			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					classNotFoundType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), classNotFoundType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

			exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(classNotFoundType.getReference(), originating), this);

		}

		return exceptionState.lub(noExceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> String getDynamicClassType(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type stringType = getProgram().getTypes().getStringType();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefExpr = new HeapDereference(stringType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				GhostTypeLookupOperator.INSTANCE,
				getLocation());

		analysis.satisfies(state, un, originating);
		String dynamicTypeStr = JavaClassType.getDynamicTypeLookup();

		return dynamicTypeStr;
	}

}
