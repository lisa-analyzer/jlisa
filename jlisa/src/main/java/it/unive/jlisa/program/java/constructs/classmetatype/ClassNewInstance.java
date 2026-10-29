package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
		ScopeId<A> context = interprocedural.getCurrentContext();

		ExpressionSet classes = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, expr, getLocation()),
				context, this);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression clazz : classes) {
			result = result.lub(innerNewInstance(interprocedural, state, clazz, expressions));
		}
		return result;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> innerNewInstance(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();

		Type stringType = getProgram().getTypes().getStringType();

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);

		AccessChild accessName = new AccessChild(stringType, expr, nameVar, location);

		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

		ExpressionSet execExpressions = new ExpressionSet();

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();
		AnalysisState<A> noSuchMethodState = state.bottom();
		Error noSuchMethodError = null;
		AnalysisState<A> instantiationState = state.bottom();
		Error instantiationError = null;

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = extractConstraints(interprocedural, state,
				accessValue);
		if (constraints == null)
			return state.topExecution();

		List<BinaryExpression> constraintList = constraints.toList();
		if (constraintList.isEmpty())
			return state;

		for (BinaryExpression constraint : constraintList) {

			String dynamicTypeStr = (String) ((Constant) constraint.getLeft()).getValue();
			dynamicTypeStr = dynamicTypeStr.replace('$', '.');

			// if this fails, throw a `InstantiationException`, meaning that
			// the class is an abstract class, or interface etc...
			boolean canInstantiate = true;
			Type dynamicType = null;
			try {
				dynamicType = JavaClassType.lookup(dynamicTypeStr);
			} catch (IllegalArgumentException e) {
				canInstantiate = false;
			}

			if (canInstantiate) {
				assert (dynamicType instanceof UnitType);
				if (dynamicType instanceof UnitType ut) {
					CompilationUnit cu = ut.getUnit();

					if (!hasDefaultCtor(cu)) {
						noSuchMethodState = noSuchMethodState
								.lub(prepareNoSuchMethodException(interprocedural, state, expressions));
						noSuchMethodError = new Error(JavaClassType.getNoSuchMethodException().getReference(),
								originating);
						continue;
					}
				}

				JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) location,
						new JavaReferenceType(dynamicType),
						new Expression[0]);
				AnalysisState<
						A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
								expressions);

				execExpressions = execExpressions.lub(callState.getExecutionExpressions());
				noExceptionState = noExceptionState.lub(callState);
			} else {
				JavaClassType instantiationExceptionType = JavaClassType.getInstantiationException();

				JavaNewObj call = new JavaNewObj(getCFG(), location,
						instantiationExceptionType.getReference(), new Expression[0]);
				AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
						expressions);

				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), instantiationExceptionType.getReference(), location);
				callState = analysis.assign(callState, throwVar,
						callState.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				callState = callState.forgetIdentifiers(call.getMetaVariables(), this);
				callState = callState.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

				instantiationState = instantiationState.lub(callState.withExecutionExpression(throwVar));
				instantiationError = new Error(instantiationExceptionType.getReference(), originating);

			}
		}

		noExceptionState = noExceptionState.withExecutionExpressions(execExpressions);
		AnalysisState<A> result = noExceptionState.lub(exceptionState);
		if (noSuchMethodError != null)
			result = analysis.moveExecutionToError(result, noSuchMethodState, noSuchMethodError, context, this);
		if (instantiationError != null)
			result = analysis.moveExecutionToError(result, instantiationState, instantiationError, context, this);
		return result;

		// TODO: IllegalAccessException

	}

	@SuppressWarnings({ "rawtypes" })
	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr)
					throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		if (!(analysis.domain instanceof SimpleAbstractDomain))
			return null;
		SimpleAbstractDomain<?, ?, ?> innerDomain = (SimpleAbstractDomain<?, ?, ?>) analysis.domain;

		assert (innerDomain != null);
		ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

		SimpleAbstractState simpleAbstractState = (SimpleAbstractState) state.getExecutionState();

		ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

		SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState, context);

		ExpressionSet rewritten = analysis.rewrite(state, expr, context, this);

		return StreamSupport.stream(rewritten.spliterator(), false)
				.map(ex -> (ValueExpression) ex)
				.flatMap(vex -> {
					try {
						return vdom.constraints(null, env, vex, this, oracle).stream();
					} catch (SemanticException e) {
						return null;
					}
				});
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> prepareNoSuchMethodException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		JavaClassType noSuchMethodType = JavaClassType.getNoSuchMethodException();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				noSuchMethodType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), noSuchMethodType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

		return state.withExecutionExpression(throwVar);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private boolean hasDefaultCtor(
			CompilationUnit cu) {
		String simpleName = cu.getName().contains(".")
				? cu.getName().substring(cu.getName().lastIndexOf(".") + 1)
				: cu.getName();

		Collection<CodeMember> ctors = cu.getInstanceCodeMembersByName(simpleName, false);
		for (CodeMember ctor : ctors) {
			if (ctor.getDescriptor().getFormals().length == 1)
				return true;
		}
		return false;
	}
}
