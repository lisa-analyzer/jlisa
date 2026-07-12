package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaIsClassDefinedOperator;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
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

		Type stringType = getProgram().getTypes().getStringType();
		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);

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

			Set<BinaryExpression> constraints = new HashSet<>();

			try {

				Class<?> c = Reachability.class;
				Field f = c.getDeclaredField("domain");

				f.setAccessible(true);

				SimpleAbstractDomain<?, ?, ?> innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);

				ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

				Object executionState = state.getExecutionState();
				ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

				SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

				ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

				SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

				ValueExpression ex = (ValueExpression) analysis.rewrite(state, accessExpr, this).iterator().next();

				constraints = vdom.constraints(null, env, ex, this, oracle);
			}
			catch (Exception e) {
			}

			for (BinaryExpression constraint : constraints) {

				String clazzName = (String)((Constant)constraint.getLeft()).getValue();
				UnitType t = getTypeFromStr(clazzName);

				// TODO AP: static initializer goes here
				// ClassUnit classUnit = (ClassUnit) t.getUnit();
				// if (classUnit.getCodeMembersByName(t.toString()).isEmpty()) {
				// 	Set<CompilationUnit> superClasses = classUnit
				// 			.getImmediateAncestors().stream()
				// 			.filter(u -> u instanceof ClassUnit)
				// 			.collect(Collectors.toSet());
				//
				// 	classUnit = (ClassUnit) superClasses.stream().findFirst().orElse(classUnit);
				// }
				// state = InitializedClassSet.initialize(state, new JavaReferenceType(t), this, interprocedural);

				LoadClass loadClass = new LoadClass(t, clazzName, cfg, location);
				AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

				ExpressionSet clazz = callState.getExecutionExpressions();

				InternalInitClassMetaObject initClazz = new InternalInitClassMetaObject(cfg, location, t);
				AnalysisState<A> initState = initClazz.forwardSemanticsAux(interprocedural, callState, new ExpressionSet[] {clazz}, expressions);

				for (SymbolicExpression c : clazz) {
					noExceptionState = noExceptionState.lub(analysis.smallStepSemantics(initState, c, this));
				}
			}
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

	private UnitType getTypeFromStr(String clazzName) {

		clazzName = clazzName.replace('$', '.');

		// NOTE: `Class.forName` cannot access `Class` of primitive types. For that the class literal is needed

		JavaClassType foundClass = null;
		JavaInterfaceType foundInterface = null;

		try {
			foundClass = JavaClassType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
		}
		try {
			foundInterface = JavaInterfaceType.lookup(clazzName);
		} catch (IllegalArgumentException e) {
		}

		UnitType t = (foundClass != null) ? foundClass : foundInterface;
		return t;
	}

}

