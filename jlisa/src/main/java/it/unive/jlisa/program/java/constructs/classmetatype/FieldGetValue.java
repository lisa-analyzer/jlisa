package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.literal.ByteLiteral;
import it.unive.jlisa.program.cfg.statement.literal.CharLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.cfg.statement.literal.ShortLiteral;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.jlisa.type.JavaTypeSystem;
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
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
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

public class FieldGetValue extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public FieldGetValue(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static FieldGetValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FieldGetValue(cfg, location, params[0], params[1]);
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
		CFG cfg = getCFG();

		ExpressionSet classes = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, left, getLocation()),
				this);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression clazz : classes) {
			result = result.lub(getValue(interprocedural, state, clazz, right, expressions));
		}
		return result;
	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> getValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", location);

		SymbolicExpression derefField = left;
		AccessChild accessName = new AccessChild(refStringType, derefField, nameVar, location);

		// access field name
		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessFieldNameValue = new AccessChild(refStringType, derefName, valueVar, location);

		// access field clazz
		AccessChild accessClazz = new AccessChild(refClassMetaType, derefField, clazzVar, location);
		HeapDereference derefClazz = new HeapDereference(classMetaType, accessClazz, location);

		AccessChild accessClazzName = new AccessChild(refStringType, derefClazz, nameVar, location);
		HeapDereference derefClazzName = new HeapDereference(stringType, accessClazzName, location);
		AccessChild accessClazzNameValue = new AccessChild(refStringType, derefClazzName, valueVar, location);

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> fieldNameStream = extractConstraints(interprocedural,
				state, accessFieldNameValue);
		if (fieldNameStream == null)
			return state.topExecution();
		List<it.unive.lisa.symbolic.value.BinaryExpression> fieldNameConstraints = fieldNameStream.toList();

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> clazzNameStream = extractConstraints(interprocedural,
				state, accessClazzNameValue);
		if (clazzNameStream == null)
			return state.topExecution();
		List<it.unive.lisa.symbolic.value.BinaryExpression> clazzNameConstraints = clazzNameStream.toList();

		AnalysisState<A> result = state.bottomExecution();

		for (it.unive.lisa.symbolic.value.BinaryExpression clazzNameConstraint : clazzNameConstraints) {

			String clazzName = (String) ((Constant) clazzNameConstraint.getLeft()).getValue();
			clazzName = clazzName.replace('$', '.');
			Unit clazzUnit = getProgram().getUnit(clazzName);

			assert (clazzUnit != null);
			assert (clazzUnit instanceof CompilationUnit);

			UnitType t = getTypeFromStr(clazzName);
			CompilationUnit compUnit = (CompilationUnit) clazzUnit;
			state = InitializedClassSet.initialize(state, new JavaReferenceType(t), this, interprocedural);

			for (it.unive.lisa.symbolic.value.BinaryExpression fieldNameConstraint : fieldNameConstraints) {

				String fieldName = (String) ((Constant) fieldNameConstraint.getLeft()).getValue();

				Global reflectedGlobal;
				if (clazzUnit instanceof ClassUnit cu) {
					reflectedGlobal = cu.getInstanceGlobal(fieldName, false);
					if (reflectedGlobal == null)
						reflectedGlobal = cu.getGlobal(fieldName);
				} else if (clazzUnit instanceof InterfaceUnit iu) {
					reflectedGlobal = iu.getGlobal(fieldName);
				} else {
					return state.topExecution();
				}

				if (reflectedGlobal == null)
					continue;

				Type reflectedFieldType = reflectedGlobal.getStaticType();

				SymbolicExpression access;
				if (reflectedGlobal.isInstance()) {

					// get the runtime types of the target object
					Set<Type> targetTypes = analysis.getRuntimeTypesOf(state, right, this);

					// remove null, if present
					boolean mightBeNull = targetTypes.remove(new JavaReferenceType(NullType.INSTANCE));

					AnalysisState<A> accessState = state.bottomExecution();

					if (mightBeNull) {
						// create NullPointerException for the null case
						JavaClassType npeType = JavaClassType.getNullPointerExceptionType();

						JavaNewObj npeCall = new JavaNewObj(cfg, location,
								npeType.getReference(), new Expression[0]);
						AnalysisState<A> nullAnalysisState = npeCall.forwardSemanticsAux(interprocedural, state,
								new ExpressionSet[0], expressions);

						CFGThrow throwVar = new CFGThrow(cfg, npeType.getReference(), location);
						nullAnalysisState = analysis.assign(nullAnalysisState, throwVar,
								nullAnalysisState.getExecutionExpressions().elements.stream().findFirst().get(), this);

						nullAnalysisState = nullAnalysisState.forgetIdentifiers(npeCall.getMetaVariables(), this);
						nullAnalysisState = nullAnalysisState
								.forgetIdentifiers(getRight().getMetaVariables(), this);

						AnalysisState<A> npeState = analysis.moveExecutionToError(
								nullAnalysisState.withExecutionExpression(throwVar),
								new Error(npeType.getReference(), originating), this);
						accessState = accessState.lub(npeState);
					}

					// if there are non-null targets, proceed normally
					if (!targetTypes.isEmpty()) {
						// safety: getRight() is always of Object type
						JavaReferenceType targetType = (JavaReferenceType) getRight().getStaticType();

						HeapDereference derefTarget = new HeapDereference(targetType.getInnerType(), right, location);
						SymbolicExpression fieldVar = reflectedGlobal.toSymbolicVariable(location);

						access = new AccessChild(reflectedFieldType, derefTarget, fieldVar, location);

						if (reflectedFieldType.isReferenceType()) {
							access = new HeapReference(reflectedFieldType, access, location);
						}

						AnalysisState<A> okState = analysis.smallStepSemantics(state, access, this);
						AnalysisState<A> boxedState = getBoxedState(interprocedural, okState, reflectedFieldType,
								expressions);
						accessState = accessState.lub(boxedState);
					}

					result = result.lub(accessState);
				} else {
					access = new GlobalVariable(
							reflectedGlobal.getStaticType(),
							reflectedGlobal.getContainer().getName() + "::" + reflectedGlobal.getName(),
							reflectedGlobal.getAnnotations(),
							location);

					if (reflectedFieldType.isReferenceType()) {
						access = new HeapReference(reflectedFieldType, access, location);
					}

					AnalysisState<A> accessedFieldState = analysis.smallStepSemantics(state, access, this);
					AnalysisState<A> boxedState = getBoxedState(interprocedural, accessedFieldState,
							reflectedFieldType, expressions);
					result = result.lub(boxedState);
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> getBoxedState(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			Type reflectedFieldType,
			StatementStore<A> expressions)
			throws SemanticException {

		CFG cfg = getCFG();
		CodeLocation location = getLocation();

		if (JavaTypeSystem.PRIMITIVE_TYPES.contains(reflectedFieldType)) {

			ExpressionSet processedReturnValues = new ExpressionSet();
			AnalysisState<A> boxedState = state.bottomExecution();

			for (SymbolicExpression returnValue : state.getExecutionExpressions()) {

				JavaReferenceType boxedType = new JavaReferenceType(JavaClassType.getWrappedType(reflectedFieldType));

				// this is a placeholder literal of the return type of the just
				// invoked method.
				// Its value has no meaning, but is used by the resolve call
				// inside
				// JavaNewObj to "choose" the appropriate constructor
				Literal<?> placeholderLiteral = getPlaceholderLiteral(reflectedFieldType, cfg, location);
				expressions.put(placeholderLiteral, state);

				JavaNewObj box = new JavaNewObj(cfg, location, boxedType, new Expression[] { placeholderLiteral });

				ExpressionSet[] ctorParam = new ExpressionSet[] { new ExpressionSet(returnValue) };
				AnalysisState<A> t = box.forwardSemanticsAux(interprocedural, state, ctorParam, expressions);

				processedReturnValues = processedReturnValues.lub(t.getExecutionExpressions());
				boxedState = boxedState.lub(t);
			}
			return boxedState.withExecutionExpressions(processedReturnValues);
		} else {
			return state;
		}
	}

	private Literal<?> getPlaceholderLiteral(
			Type t,
			CFG cfg,
			CodeLocation location) {
		if (t == JavaIntType.INSTANCE)
			return new IntLiteral(cfg, location, 0);
		if (t == JavaFloatType.INSTANCE)
			return new FloatLiteral(cfg, location, 0.0F);
		if (t == JavaDoubleType.INSTANCE)
			return new DoubleLiteral(cfg, location, 0.0);
		if (t == JavaLongType.INSTANCE)
			return new LongLiteral(cfg, location, 0);
		if (t == JavaCharType.INSTANCE)
			return new CharLiteral(cfg, location, 0);
		if (t == JavaByteType.INSTANCE)
			return new ByteLiteral(cfg, location, 0);
		if (t == JavaShortType.INSTANCE)
			return new ShortLiteral(cfg, location, 0);
		if (t == JavaBooleanType.INSTANCE)
			return new TrueLiteral(cfg, location);

		return null;
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

	private UnitType getTypeFromStr(
			String clazzName) {

		clazzName = clazzName.replace('$', '.');
		Type t = getProgram().getTypes().getType(clazzName);

		if (!(t instanceof UnitType))
			return null;

		return (UnitType) t;
	}
}
