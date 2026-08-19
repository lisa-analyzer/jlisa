package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.jlisa.analysis.JavaReachability;
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
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FieldSetValue extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public FieldSetValue(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "set", left, middle, right);
	}

	public static FieldSetValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FieldSetValue(cfg, location, params[0], params[1], params[2]);
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

		// left is the Field object;
		// middle is the object to set the field of;
		// right is the new value

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation loc = getLocation();
		CFG cfg = getCFG();

		ExpressionSet classes = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, left, getLocation()),
				this);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression clazz : classes) {
			result = result.lub(setValue(interprocedural, state, clazz, middle, right, expressions));
		}
		return result;
	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> setValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation loc = getLocation();
		CFG cfg = getCFG();

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);
		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", loc);

		SymbolicExpression derefField = left;
		AccessChild accessName = new AccessChild(refStringType, derefField, nameVar, loc);

		// access field name
		HeapDereference derefName = new HeapDereference(stringType, accessName, loc);
		AccessChild accessFieldNameValue = new AccessChild(refStringType, derefName, valueVar, loc);

		// access field clazz
		AccessChild accessClazz = new AccessChild(refClassMetaType, derefField, clazzVar, loc);
		HeapDereference derefClazz = new HeapDereference(classMetaType, accessClazz, loc);

		AccessChild accessClazzName = new AccessChild(refStringType, derefClazz, nameVar, loc);
		HeapDereference derefClazzName = new HeapDereference(stringType, accessClazzName, loc);
		AccessChild accessClazzNameValue = new AccessChild(refStringType, derefClazzName, valueVar, loc);

		Stream<BinaryExpression> fieldNameStream = extractConstraints(interprocedural, state, accessFieldNameValue);
		if (fieldNameStream == null)
			return state.topExecution();
		List<BinaryExpression> fieldNameConstraints = fieldNameStream.toList();

		Stream<BinaryExpression> clazzNameStream = extractConstraints(interprocedural, state, accessClazzNameValue);
		if (clazzNameStream == null)
			return state.topExecution();
		List<BinaryExpression> clazzNameConstraints = clazzNameStream.toList();

		AnalysisState<A> result = state;

		for (BinaryExpression clazzNameConstraint : clazzNameConstraints) {

			String clazzName = (String) ((Constant) clazzNameConstraint.getLeft()).getValue();
			clazzName = clazzName.replace('$', '.');
			Unit clazzUnit = getProgram().getUnit(clazzName);

			assert (clazzUnit != null);
			assert (clazzUnit instanceof CompilationUnit);

			UnitType ut = getTypeFromStr(clazzName);
			CompilationUnit compUnit = (CompilationUnit) clazzUnit;
			state = InitializedClassSet.initialize(state, new JavaReferenceType(ut), this, interprocedural);

			for (BinaryExpression fieldNameConstraint : fieldNameConstraints) {

				String fieldName = (String) ((Constant) fieldNameConstraint.getLeft()).getValue();

				Global reflectedGlobal;
				if (clazzUnit instanceof ClassUnit cu) {
					reflectedGlobal = cu.getInstanceGlobal(fieldName, false);
					if (reflectedGlobal == null)
						reflectedGlobal = cu.getGlobal(fieldName);
				} else if (clazzUnit instanceof InterfaceUnit iu)
					reflectedGlobal = iu.getGlobal(fieldName);
				else
					return state.topExecution();

				if (reflectedGlobal == null)
					return state.topExecution();

				Type reflectedFieldType = reflectedGlobal.getStaticType();

				if (reflectedGlobal.isInstance()) {

					// get the runtime types of the target object
					Set<Type> targetTypes = analysis.getRuntimeTypesOf(state, middle, this);

					// remove null, if present
					boolean mightBeNull = targetTypes.remove(new JavaReferenceType(NullType.INSTANCE));

					// AnalysisState<A> setState = state.bottomExecution();

					if (mightBeNull) {
						// create NullPointerException for the null case
						JavaClassType npeType = JavaClassType.getNullPointerExceptionType();

						JavaNewObj npeCall = new JavaNewObj(cfg, loc,
								npeType.getReference(), new Expression[0]);
						AnalysisState<A> nullAnalysisState = npeCall.forwardSemanticsAux(interprocedural, result,
								new ExpressionSet[0], expressions);

						CFGThrow throwVar = new CFGThrow(cfg, npeType.getReference(), loc);
						nullAnalysisState = analysis.assign(nullAnalysisState, throwVar,
								nullAnalysisState.getExecutionExpressions().elements.stream().findFirst().get(), this);

						nullAnalysisState = nullAnalysisState.forgetIdentifiers(npeCall.getMetaVariables(), this);
						nullAnalysisState = nullAnalysisState
								.forgetIdentifiers(getMiddle().getMetaVariables(), this);

						AnalysisState<A> npeState = analysis.moveExecutionToError(
								nullAnalysisState.withExecutionExpression(throwVar),
								new Error(npeType.getReference(), originating), this);

						result = npeState;
					}

					// if there are non-null targets, proceed normally
					if (!targetTypes.isEmpty()) {
						GlobalVariable fieldVar = new GlobalVariable(Untyped.INSTANCE, fieldName, loc);

						// safety: middle is always a subclass of Object
						JavaReferenceType targetType = (JavaReferenceType) getMiddle().getStaticType();

						HeapDereference derefTarget = new HeapDereference(targetType.getInnerType(), middle, loc);
						AccessChild access = new AccessChild(reflectedFieldType, derefTarget, fieldVar, loc);

						// NOTE: this getMiddle() is wrong, but shouldn't hurt
						// anything. It should be a fieldAccess expression
						JavaAssignment assign = new JavaAssignment(getCFG(), loc, getMiddle(), getRight());

						AnalysisState<A> t = assign.fwdBinarySemantics(interprocedural, result, access, right,
								expressions);
						result = t;
					}
				} else {
					GlobalVariable reflectedAccess = new GlobalVariable(
							reflectedGlobal.getStaticType(),
							reflectedGlobal.getContainer().getName() + "::" + reflectedGlobal.getName(),
							reflectedGlobal.getAnnotations(),
							loc);
					VariableRef target = new VariableRef(getCFG(), loc, reflectedAccess.getName(),
							reflectedGlobal.getStaticType());
					JavaAssignment assign = new JavaAssignment(getCFG(), loc, target, getRight());

					AnalysisState<A> t = assign.fwdBinarySemantics(
							interprocedural,
							result,
							reflectedAccess,
							right,
							expressions);
					result = t;
				}
			}
		}

		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Stream<it.unive.lisa.symbolic.value.BinaryExpression> extractConstraints(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr)
					throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		SimpleAbstractDomain<?, ?, ?> innerDomain;

		try {
			Class<?> c = JavaReachability.class;
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

	private UnitType getTypeFromStr(
			String clazzName) {

		clazzName = clazzName.replace('$', '.');
		Type t = getProgram().getTypes().getType(clazzName);

		if (!(t instanceof UnitType))
			return null;

		return (UnitType) t;
	}
}
