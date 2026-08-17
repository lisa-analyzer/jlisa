package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.cfg.expression.JavaArrayAccess;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.literal.ByteLiteral;
import it.unive.jlisa.program.cfg.statement.literal.CharLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.cfg.statement.literal.ShortLiteral;
import it.unive.jlisa.program.operator.IsMemberStaticOperator;
import it.unive.jlisa.program.type.*;
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
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MethodInvoke extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public MethodInvoke(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "methodInvoke", left, middle, right);
	}

	public static MethodInvoke build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MethodInvoke(cfg, location, params[0], params[1], params[2]);
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
		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		ExpressionSet methods = analysis.rewrite(state, new HeapDereference(Untyped.INSTANCE, left, getLocation()), this);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression method : methods) {
			result = result.lub(invoke(interprocedural, state, method, middle, right, expressions));
		}
		return result;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> invoke(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		Type stringType = JavaClassType.getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type methodType = JavaClassType.getMethodType();
		Type refMethodType = new JavaReferenceType(methodType);
		JavaReferenceType refObjectArrType = JavaArrayType.OBJECT_ARRAY;

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);

		SymbolicExpression derefMethod = left;
		AccessChild accessName = new AccessChild(refStringType, derefMethod, nameVar, location);

		// method->name->value
		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

		// if this is just null, throw a null pointer exception
		Set<Type> receiverMethodTypes = analysis.getRuntimeTypesOf(state, left, this);
		if (receiverMethodTypes.size() == 1 && receiverMethodTypes.contains(NullType.INSTANCE)) {
			return throwNullPointerException(interprocedural, state, expressions);
		}

		// check if method is static via modifiers
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", location);
		AccessChild accessModifiers = new AccessChild(JavaIntType.INSTANCE, derefMethod, modifiersVar,
				location);

		it.unive.lisa.symbolic.value.UnaryExpression isStaticExpr = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaBooleanType.INSTANCE, accessModifiers, IsMemberStaticOperator.INSTANCE, location);

		Satisfiability isStaticSat = Satisfiability.NOT_SATISFIED;
		isStaticSat = analysis.satisfies(state, isStaticExpr, this);

		// we don't know whether the method is static or not
		if (isStaticSat == Satisfiability.UNKNOWN)
			return state.topExecution();

		AnalysisState<A> exceptionState = state.bottomExecution();
		AnalysisState<A> noExceptionState = state.bottomExecution();

		Set<Type> thisObjTypes = analysis.getRuntimeTypesOf(state, middle, this);
		// throw a NullPointerException if the method is not static and the
		// runtime type
		// of the receiver could be null
		if (isStaticSat == Satisfiability.NOT_SATISFIED
				&& thisObjTypes.remove(new JavaReferenceType(NullType.INSTANCE))) {
			exceptionState = throwNullPointerException(interprocedural, state, expressions);
		}

		// if the receiver is certainly null, stop immediately
		if (thisObjTypes.isEmpty())
			return exceptionState;

		List<BinaryExpression> clazzNameConstraints = getClassNameConstraints(interprocedural, state, derefMethod);

		// check the types of the receiver against the type of the declaring
		// class.
		// If they do not match, throw an IllegalArgumentException
		if (isStaticSat == Satisfiability.NOT_SATISFIED) {
			boolean mayThrowIA = checkMayThrowIllegalArgumentReceiver(interprocedural, state, thisObjTypes,
					clazzNameConstraints, derefMethod);
			if (mayThrowIA) {
				exceptionState = exceptionState.lub(throwIllegalArgumentException(interprocedural, state, expressions));
			}
		}

		ArrayList<Expression> args = new ArrayList<>();
		ArrayList<ExpressionSet> symbolicArgs = new ArrayList<>();
		StatementStore<A> callExpressions = expressions.bottom();

		// if not static, add the receiver
		if (isStaticSat == Satisfiability.NOT_SATISFIED) {
			args.add(getMiddle());
			symbolicArgs.add(new ExpressionSet(middle));
			callExpressions.put(getMiddle(), state.withExecutionExpression(middle));
		}

		HeapDereference derefArr = new HeapDereference(refObjectArrType.getInnerType(), right, location);
		AccessChild lenAccess = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		Satisfiability nArgsMatch = isNArgsEq(interprocedural, state, derefMethod, lenAccess);
		if (nArgsMatch != Satisfiability.SATISFIED) {
			return throwIllegalArgumentException(interprocedural, state, expressions);
		}

		boolean outOfBoundsMethodArr = false;

		// extract all the symbolic expressions from the third argument
		// to pass them to UnresolvedCall
		for (int i = 0; outOfBoundsMethodArr == false; ++i) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
					JavaBooleanType.INSTANCE,
					idx, lenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			Expression arrExpression = getRight();
			Expression accessIdx = new IntLiteral(cfg, location, i);

			JavaArrayAccess expr = new JavaArrayAccess(cfg, Untyped.INSTANCE, location, arrExpression, accessIdx);
			args.add(expr);

			// TODO: check the types of the arguments. If one does not match
			// with the corresponding
			// formal parameter, throw an IllegalArgumentException

			AccessChild accessExpr = new AccessChild(Untyped.INSTANCE, derefArr, idx, location);
			ExpressionSet exprSet = new ExpressionSet();

			// NOTE: this is probably not needed
			for (Type t : analysis.getRuntimeTypesOf(state, accessExpr, this)) {
				accessExpr = new AccessChild(t, derefArr, idx, location);
				exprSet = exprSet.lub(new ExpressionSet(accessExpr));
			}

			callExpressions.put(expr, state.withExecutionExpressions(exprSet));

			assert (!exprSet.isBottom());
			symbolicArgs.add(exprSet);
		}

		Expression[] expressionsArgs = args.toArray(new Expression[0]);
		ExpressionSet[] symbolicExpressionsArgs = symbolicArgs.toArray(new ExpressionSet[0]);

		Stream<it.unive.lisa.symbolic.value.BinaryExpression> constraints = extractConstraints(interprocedural, state,
				accessValue);
		assert (constraints != null);
		if (constraints == null)
			return state.bottomExecution();

		for (it.unive.lisa.symbolic.value.BinaryExpression constraint : constraints.toList()) {

			String methodName = (String) ((Constant) constraint.getLeft()).getValue();

			CallType callType = (isStaticSat == Satisfiability.SATISFIED) ? CallType.STATIC : CallType.INSTANCE;

			UnresolvedCall call;
			for (BinaryExpression clazzNameConstraint : clazzNameConstraints) {
				// if the call is static we need to qualify it, thus the call
				// also depends on the qualifiers
				if (callType == CallType.STATIC) {
					String clazzName = (String) ((Constant) clazzNameConstraint.getLeft()).getValue();
					call = new UnresolvedCall(
							cfg,
							location,
							callType,
							clazzName,
							methodName,
							expressionsArgs);
				} else {
					call = new UnresolvedCall(
							cfg,
							location,
							callType,
							"",
							methodName,
							expressionsArgs);
				}

				AnalysisState<A> sem = call.forwardSemanticsAux(interprocedural, state, symbolicExpressionsArgs,
						callExpressions);

				assert (!sem.getExecutionExpressions().isEmpty());

				// if open call, it's a stray call. Throw an illegal argument exception
				if (sem.getExecutionExpressions().iterator().next().getStaticType() == Untyped.INSTANCE) {
					exceptionState = exceptionState.lub(throwIllegalArgumentException(interprocedural, state, expressions));
					continue;
				}

				// TODO: if the method call threw an exception, wrap it into a
				// InvocationException

				// if returns void, just return a null value
				if (sem.getExecutionExpressions().equals(new ExpressionSet(new Skip(location)))) {
					NullConstant nc = new NullConstant(location);

					Type refObjectType = new JavaReferenceType(JavaClassType.getObjectType());
					Set<Type> types = new HashSet<>();
					types.add(refObjectType);
					TypeTokenType t = new TypeTokenType(types);

					Constant castTo = new Constant(t, refObjectType, location);

					BinaryExpression castAs = new BinaryExpression(refObjectType, nc, castTo, TypeCast.INSTANCE,
							location);
					return analysis.smallStepSemantics(sem, castAs, this);
				}

				// if the return value(s) is of primitive type, we need to box
				// it
				AnalysisState<A> boxedState = getBoxedState(interprocedural, sem, derefMethod, expressions);

				noExceptionState = noExceptionState.lub(boxedState);

				if (callType == CallType.INSTANCE)
					break;
			}
		}

		return noExceptionState.lub(exceptionState);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability isNArgsEq(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			AccessChild right)
			throws SemanticException {

		CodeLocation location = getLocation();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaArrayType classArrType = JavaArrayType.lookup(refClassMetaType, 1);
		JavaReferenceType refClassArrType = new JavaReferenceType(classArrType);

		// left is of type Method
		GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "parameterTypes", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);

		AccessChild accessParamTypes = new AccessChild(refClassArrType, left, paramTypesVar, location);
		HeapDereference derefParamTypes = new HeapDereference(classArrType, accessParamTypes, location);
		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, derefParamTypes, lengthVar, location);

		it.unive.lisa.symbolic.value.BinaryExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				accessLen, right, ComparisonEq.INSTANCE, location);

		Satisfiability sat = analysis.satisfies(state, eq, this);
		return sat;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNullPointerException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType nullPointerExceptionType = JavaClassType.getNullPointerExceptionType();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				nullPointerExceptionType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), nullPointerExceptionType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		assert (!state.isBottom());

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(nullPointerExceptionType.getReference(), originating), this);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> List<BinaryExpression> getClassNameConstraints(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression derefMethod)
			throws SemanticException {

		CodeLocation loc = getLocation();

		GlobalVariable declaringClassVar = new GlobalVariable(Untyped.INSTANCE, "clazz", loc);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);

		AccessChild accessClazz = new AccessChild(JavaClassType.getClassMetaType().getReference(), derefMethod,
				declaringClassVar, loc);
		HeapDereference derefClazz = new HeapDereference(JavaClassType.getClassMetaType(), accessClazz, loc);

		AccessChild accessName = new AccessChild(JavaClassType.getStringType().getReference(), derefClazz, nameVar,
				loc);
		HeapDereference derefName = new HeapDereference(JavaClassType.getStringType(), accessName, loc);

		// method->declaringClass->name->value
		AccessChild accessValue = new AccessChild(JavaClassType.getStringType(), derefName, valueVar, loc);

		Stream<BinaryExpression> constraintsStream = extractConstraints(interprocedural, state, accessValue);
		assert (constraintsStream != null);

		List<BinaryExpression> clazzNameConstraints = constraintsStream.toList();
		assert (!clazzNameConstraints.isEmpty());
		return clazzNameConstraints;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean checkMayThrowIllegalArgumentReceiver(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			Set<Type> receiverTypes,
			List<BinaryExpression> clazzNameConstraints,
			SymbolicExpression derefMethod)
			throws SemanticException {

		CodeLocation loc = getLocation();

		for (BinaryExpression constraint : clazzNameConstraints) {

			String clazzName = (String) ((Constant) constraint.getLeft()).getValue();
			UnitType clazzUt = getTypeFromStr(clazzName);

			// NOTE: should never happen
			assert (clazzUt != null);
			if (clazzUt == null)
				return true;

			for (Type t : receiverTypes) {

				assert (t instanceof JavaReferenceType);
				assert (clazzUt instanceof JavaClassType || clazzUt instanceof JavaInterfaceType);
				int distance = getProgram().getTypes().distanceBetweenTypes(new JavaReferenceType(clazzUt), t);
				if (distance == -1)
					return true;
			}
		}
		return false;
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwIllegalArgumentException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType illegalArgumentExceptionType = JavaClassType.getIllegalArgumentExceptionType();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				illegalArgumentExceptionType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), illegalArgumentExceptionType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(illegalArgumentExceptionType.getReference(), originating), this);
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> getBoxedState(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression derefMethod,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CFG cfg = getCFG();
		CodeLocation location = getLocation();

		GlobalVariable returnTypeVar = new GlobalVariable(Untyped.INSTANCE, "returnType", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);

		JavaClassType stringType = JavaClassType.getStringType();
		JavaClassType classType = JavaClassType.getClassMetaType();

		AccessChild accessReturnType = new AccessChild(classType.getReference(), derefMethod, returnTypeVar, location);
		HeapDereference derefRetType = new HeapDereference(classType, accessReturnType, location);

		AccessChild accessName = new AccessChild(stringType.getReference(), derefRetType, nameVar, location);
		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

		ExpressionSet returnValues = state.getExecutionExpressions();
		assert (!returnValues.isEmpty());
		ExpressionSet processedReturnValues = new ExpressionSet();

		AnalysisState<A> res = state.bottomExecution();

		Stream<BinaryExpression> returnTypeStream = extractConstraints(interprocedural, state, accessValue);
		assert (returnTypeStream != null);
		if (returnTypeStream == null)
			return state.topExecution();

		List<BinaryExpression> returnTypeConstraints = returnTypeStream.toList();
		assert (!returnTypeConstraints.isEmpty());

		for (BinaryExpression constraint : returnTypeConstraints) {

			String returnTypeStr = (String) ((Constant) constraint.getLeft()).getValue();

			Type exprType = getProgram().getTypes().getType(returnTypeStr);

			// this should never happen. Method return types are loaded via
			// constants
//			assert (exprType != null);
			if (exprType == null)
				continue;

			for (SymbolicExpression returnValue : returnValues) {

				AnalysisState<A> boxedState = state.bottomExecution();

				if (JavaTypeSystem.PRIMITIVE_TYPES.contains(exprType)) {

					JavaReferenceType boxedType = new JavaReferenceType(JavaClassType.getWrappedType(exprType));

					// this is a placeholder literal of the return type of the
					// just invoked method.
					// Its value has no meaning, but is used by the resolve call
					// inside
					// JavaNewObj to "choose" the appropriate constructor
					Literal<?> placeholderLiteral = getPlaceholderLiteral(exprType, cfg, location);
					expressions.put(placeholderLiteral, state);

					JavaNewObj box = new JavaNewObj(cfg, location, boxedType, new Expression[] { placeholderLiteral });

					ExpressionSet[] ctorParam = new ExpressionSet[] { new ExpressionSet(returnValue) };
					AnalysisState<A> t = box.forwardSemanticsAux(interprocedural, state, ctorParam, expressions);

					assert (!t.getExecutionExpressions().isEmpty());

					processedReturnValues = processedReturnValues.lub(t.getExecutionExpressions());
					boxedState = boxedState.lub(t);

					expressions.forget(placeholderLiteral);
				} else {
					boxedState = boxedState.lub(state);
					processedReturnValues = processedReturnValues.lub(new ExpressionSet(returnValue));
				}

				assert (!boxedState.isBottom());
				res = res.lub(boxedState);
			}
		}

		assert (!processedReturnValues.isBottom());
		assert (!res.isBottom());
		return res.withExecutionExpressions(processedReturnValues);
	}

}
