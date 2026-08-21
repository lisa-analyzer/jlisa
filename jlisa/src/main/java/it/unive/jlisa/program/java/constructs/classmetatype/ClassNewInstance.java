package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.ReachabilityProduct;
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

		ExpressionSet classes = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, expr, getLocation()),
				this);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression clazz : classes) {
			result = result.lub(innerNewInstance(interprocedural, state, expr, expressions));
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
		CodeLocation location = getLocation();

		Type stringType = getProgram().getTypes().getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference derefClazz = new HeapDereference(classMetaType, expr, location);
		AccessChild accessName = new AccessChild(stringType, derefClazz, nameVar, location);

		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

		ExpressionSet execExpressions = new ExpressionSet();

		SimpleAbstractDomain<?, ?, ?> innerDomain;

		try {
			Class<?> c = Reachability.class;
			Field f = c.getDeclaredField("domain");

			f.setAccessible(true);

			innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);
		} catch (Exception e) {
			return state.topExecution();
		}

		assert (innerDomain != null);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = extractConstraints(interprocedural, state,
				accessValue);
		if (constraints == null)
			return state.topExecution();

		for (BinaryExpression constraint : constraints.toList()) {

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
						exceptionState = exceptionState
								.lub(throwNoSuchMethodException(interprocedural, state, expressions));
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
						callState.getExecutionExpressions().elements.stream().findFirst().get(), this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				callState = callState.forgetIdentifiers(call.getMetaVariables(), this);
				callState = callState.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

				exceptionState = exceptionState
						.lub(analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
								new Error(instantiationExceptionType.getReference(), originating), this));

			}
		}

		noExceptionState = noExceptionState.withExecutionExpressions(execExpressions);
		return noExceptionState.lub(exceptionState);

		// TODO: IllegalAccessException

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr)
					throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		SimpleAbstractDomain<?, ?, ?> innerDomain;

		try {
			Class<?> c = Reachability.class;
			Field f = c.getDeclaredField("domain");

			f.setAccessible(true);

			innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);
		} catch (Exception e) {
			return null;
		}

		assert (innerDomain != null);
		ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

		Object executionState = state.getExecutionState();
		ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

		SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

		ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

		SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

		ExpressionSet rewritten = analysis.rewrite(state, expr, this);

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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNoSuchMethodException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType noSuchMethodType = JavaClassType.getNoSuchMethodException();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				noSuchMethodType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), noSuchMethodType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(noSuchMethodType.getReference(), originating), this);
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
