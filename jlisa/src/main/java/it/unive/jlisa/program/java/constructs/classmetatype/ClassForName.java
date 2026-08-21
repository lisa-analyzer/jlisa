package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaIsClassDefinedOperator;
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
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
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
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
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
		CodeLocation location = getLocation();
		CFG cfg = getCFG();
		TypeSystem typeSystem = getProgram().getTypes();

		Type stringType = typeSystem.getStringType();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference derefExpr = new HeapDereference(stringType, expr, location);
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, location);

		// check if class actually exists
		it.unive.lisa.symbolic.value.UnaryExpression isClassDefined = new it.unive.lisa.symbolic.value.UnaryExpression(
				stringType,
				accessExpr,
				JavaIsClassDefinedOperator.INSTANCE,
				location);

		Satisfiability sat = analysis.satisfies(state, isClassDefined, originating);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// populate the "no exception" path
		if (sat != Satisfiability.NOT_SATISFIED) {

			Stream<BinaryExpression> constraints = extractConstraints(interprocedural, state, accessExpr);
			if (constraints == null)
				return state.topExecution();

			AnalysisState<A> tmp = state;
			ExpressionSet execExpressions = new ExpressionSet();

			for (BinaryExpression constraint : constraints.toList()) {

				String clazzName = (String) ((Constant) constraint.getLeft()).getValue();
				UnitType t = getTypeFromStr(clazzName);

				// we are in a unknown case and the class doesn't exist
				if (t == null)
					continue;

				// static initializer
				CompilationUnit cu = t.getUnit();
				tmp = InitializedClassSet.initialize(tmp, new JavaReferenceType(t), this, interprocedural);

				for (CompilationUnit ancestorCu : cu.getImmediateAncestors().stream().toList()) {

					if (ancestorCu instanceof InterfaceUnit) {
						// TODO: only call the static initializer iff the
						// interface contains a default method
						continue;
					}

					Type ancestorType = typeSystem.getType(ancestorCu.getName());
					assert (ancestorType instanceof UnitType);
					tmp = InitializedClassSet.initialize(tmp, new JavaReferenceType(ancestorType), this,
							interprocedural);
				}

				LoadClass loadClass = new LoadClass(t, clazzName, cfg, location);
				AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0],
						expressions);

				ExpressionSet clazz = callState.getExecutionExpressions();

				InternalInitClassMetaObject initClazz = new InternalInitClassMetaObject(cfg, location, t, this);
				AnalysisState<A> initState = initClazz.forwardSemanticsAux(interprocedural, callState,
						new ExpressionSet[] { clazz }, expressions);

				tmp = initState;

				execExpressions = execExpressions.lub(clazz);
			}

			if (tmp != state)
				noExceptionState = tmp.withExecutionExpressions(execExpressions);
		}

		// `ClassNotFoundException to be thrown
		if (sat != Satisfiability.SATISFIED) {

			JavaClassType classNotFoundType = JavaClassType.getClassNotFoundException();

			JavaNewObj call = new JavaNewObj(cfg, location,
					classNotFoundType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(cfg, classNotFoundType.getReference(), location);
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

	private UnitType getTypeFromStr(
			String clazzName) {

		clazzName = clazzName.replace('$', '.');

		// NOTE: `Class.forName` cannot access `Class` of primitive types. For
		// that the class literal is needed
		Type t = getProgram().getTypes().getType(clazzName);

		if (!(t instanceof UnitType))
			return null;

		return (UnitType) t;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
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

}
