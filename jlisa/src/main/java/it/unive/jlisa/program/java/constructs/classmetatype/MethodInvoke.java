package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.cfg.expression.JavaArrayAccess;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.cfg.statement.literal.FloatLiteral;
import it.unive.jlisa.program.cfg.statement.literal.LongLiteral;
import it.unive.jlisa.program.cfg.statement.literal.DoubleLiteral;
import it.unive.jlisa.program.cfg.statement.literal.CharLiteral;
import it.unive.jlisa.program.cfg.statement.literal.ByteLiteral;
import it.unive.jlisa.program.cfg.statement.literal.ShortLiteral;
import it.unive.jlisa.program.type.JavaArrayType;
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
import it.unive.lisa.program.cfg.statement.InstrumentedReceiverRef;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

		Type stringType = JavaClassType.getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type methodType = JavaClassType.getMethodType();
		JavaReferenceType refObjectArrType = JavaArrayType.OBJECT_ARRAY;

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);

		HeapDereference derefMethod = new HeapDereference(methodType, left, location);
		AccessChild accessName = new AccessChild(refStringType, derefMethod, nameVar, location);

		// method->name->value
		HeapDereference derefName = new HeapDereference(stringType, accessName, location);
		AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);


		Set<Type> thisObjTypes = analysis.getRuntimeTypesOf(state, middle, this);
		// TODO AP: temporary assumption
		assert(thisObjTypes.size() == 1);



		Type thisObjType = thisObjTypes.iterator().next();

		if (thisObjType instanceof JavaReferenceType jrt)
			thisObjType = jrt.getInnerType();

		// TODO check that the type can be a receiver of the method call, else throw the exception

		String clazz = thisObjType.toString();

		// extract all the symbolic expressions from the third argument
		// and pass them to UnresolvedCall

		ArrayList<Expression> args = new ArrayList<>();
		args.add(getMiddle());

		ArrayList<ExpressionSet> symbolicArgs = new ArrayList<>();
		symbolicArgs.add(new ExpressionSet(middle));

		HeapDereference derefArr = new HeapDereference(refObjectArrType.getInnerType(), right, location);
		AccessChild lenAccess = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		boolean outOfBoundsMethodArr = false;

		// stop when we are out of bounds
		for (int i = 0; outOfBoundsMethodArr == false; ++i) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression( JavaBooleanType.INSTANCE,
				idx, lenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			Expression arrExpression = getRight();
			Expression accessIdx = new IntLiteral(cfg, location, i);

			JavaArrayAccess expr = new JavaArrayAccess(cfg, location, arrExpression, accessIdx);

			args.add(expr);

			AccessChild accessExpr = new AccessChild(Untyped.INSTANCE, derefArr, idx, location);
			ExpressionSet exprSet = new ExpressionSet();
			for (Type t : analysis.getRuntimeTypesOf(state, accessExpr, this)) {
				accessExpr = new AccessChild(t, derefArr, idx, location);
				exprSet = exprSet.lub(new ExpressionSet(accessExpr));
			}
			symbolicArgs.add(exprSet);
		}

		Expression[] expressionsArgs = args.toArray(new Expression[0]);
		ExpressionSet[] symbolicExpressionsArgs = symbolicArgs.toArray(new ExpressionSet[0]);

		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = getConstraints(analysis, state, accessValue);

		// TODO AP: temporary assumption, just one value
		assert(constraints.size() == 1);
		it.unive.lisa.symbolic.value.BinaryExpression constraint = constraints.iterator().next();

		String methodName = (String)((Constant)constraint.getLeft()).getValue();

		UnresolvedCall call = new UnresolvedCall(
				cfg,
				location,
				CallType.INSTANCE,
				clazz,
				methodName,
				expressionsArgs);
		AnalysisState<A> sem = call.forwardSemanticsAux(interprocedural, state, symbolicExpressionsArgs, expressions);

		// if returns void, just return a null value
		if (sem.getExecutionExpressions().equals(new ExpressionSet(new Skip(location)))) {
			NullConstant nc = new NullConstant(location);

			Type refObjectType = new JavaReferenceType(JavaClassType.getObjectType());
			Set<Type> types = new HashSet<>();
			types.add(refObjectType);
			TypeTokenType t = new TypeTokenType(types);

			Constant castTo = new Constant(t, refObjectType, location);

			BinaryExpression castAs = new BinaryExpression(refObjectType, nc, castTo, TypeCast.INSTANCE, location);
			return analysis.smallStepSemantics(sem, castAs, this);
		}

		ExpressionSet returnValues = sem.getExecutionExpressions();
		ExpressionSet processedReturnValues = new ExpressionSet();

		// TODO could return more than one value
		SymbolicExpression returnValue = returnValues.iterator().next();
		Type exprType = analysis.getRuntimeTypesOf(sem, returnValue, this).iterator().next();

		AnalysisState<A> boxedState = sem.bottomExecution();

		if (JavaTypeSystem.PRIMITIVE_TYPES.contains(exprType)) {

			JavaReferenceType boxedType = new JavaReferenceType(JavaClassType.getWrappedType(exprType));

			// this is a placeholder literal of the return type of the just invoked method.
			// Its value has no meaning, but is used by the resolve call inside
			// JavaNewObj to "choose" the appropriate constructor
			Literal<?> placeholderLiteral = getPlaceholderLiteral(exprType, cfg, location);
			expressions.put(placeholderLiteral, sem);

			JavaNewObj box = new JavaNewObj(cfg, location, boxedType, new Expression[] {placeholderLiteral});

			ExpressionSet[] ctorParam = new ExpressionSet[] {new ExpressionSet(returnValue) };
			AnalysisState<A> t = box.forwardSemanticsAux(interprocedural, sem, ctorParam, expressions);

			processedReturnValues = processedReturnValues.lub(t.getExecutionExpressions());
			boxedState = boxedState.lub(t);
		}
		else {
			boxedState = boxedState.lub(sem);
			processedReturnValues = processedReturnValues.lub(new ExpressionSet(returnValue));
		}

		return boxedState.withExecutionExpressions(processedReturnValues);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<it.unive.lisa.symbolic.value.BinaryExpression> getConstraints(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expr) {

		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = new HashSet<>();

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

			ValueExpression ex = (ValueExpression) analysis.rewrite(state, expr, this).iterator().next();

			constraints = vdom.constraints(null, env, ex, this, oracle);
		}
		catch (Exception e) {
		}

		return constraints;
	}


	private Literal<?> getPlaceholderLiteral(Type t, CFG cfg, CodeLocation location) {
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

}
