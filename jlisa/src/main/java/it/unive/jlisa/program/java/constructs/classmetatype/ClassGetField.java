package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.program.CachedReflectionDataSet;
import it.unive.jlisa.program.LoadedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

public class ClassGetField extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected ClassGetField(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "getField", left, right);
	}

	public static ClassGetField build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetField(cfg, location, params[0], params[1]);
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

		Set<Type> clazzTypes = analysis.getRuntimeTypesOf(state, left, this);
		AnalysisState<A> result = state;

		for (Type t : clazzTypes) {
			if (t instanceof JavaReferenceType jrt && jrt.getInnerType().isNullType()) {
				result = result.lub(throwNoSuchFieldException(interprocedural, state, expressions));
			}
			// search the field
			else {
				AnalysisState<A> fieldSearched = searchField(interprocedural, state, left, right, expressions);

				result = result.lub(fieldSearched);
			}
		}

		return result;
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



	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> searchField(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = getProgram().getTypes().getStringType();
		Type refStringType = new JavaReferenceType(stringType);
		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type refFieldMetaType = new JavaReferenceType(fieldMetaType);
		Type fieldArr = JavaArrayType.lookup(refFieldMetaType, 1);
		Type classMetaType = JavaClassType.getClassMetaType();
		Type refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable superClassVar = new GlobalVariable(Untyped.INSTANCE, "superClass", location);

		HeapDereference derefClazz = new HeapDereference(classMetaType, left, location);

		AccessChild accessClazzName = new AccessChild(refStringType, derefClazz, nameVar, location);
		HeapDereference derefClazzName = new HeapDereference(stringType, accessClazzName, location);
		AccessChild accessClazzNameValue = new AccessChild(stringType, derefClazzName, valueVar, location);

		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = getConstraints(analysis, state, accessClazzNameValue);

		AnalysisState<A> tmp = state;
		for (it.unive.lisa.symbolic.value.BinaryExpression constraint : constraints) {

			String clazzName = (String)((Constant)constraint.getLeft()).getValue();
			UnitType t = getTypeFromStr(clazzName);

			// cache reflection data if necessary
			if (!CachedReflectionDataSet.isClassReflectionDataCached(state, t)) {

				assert(LoadedClassSet.isClassLoaded(state, t));
				ExpressionSet clazz = new ExpressionSet(LoadedClassSet.getLoadedClassHandle(t, location));

				InternalInitClassMetaObject initClazz = new InternalInitClassMetaObject(getCFG(), location, t);
				AnalysisState<A> initState = initClazz.forwardSemanticsAux(interprocedural, state, new ExpressionSet[] {clazz}, expressions);

				tmp = tmp.lub(initState);
			}
		}

		state = tmp;

		// access field name (2nd arg)
		HeapDereference derefFieldNameExpr = new HeapDereference(stringType, right, location);
		AccessChild accessFieldNameExpr = new AccessChild(stringType, derefFieldNameExpr, valueVar, location);

		GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", location);
		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);

		// get number of fields
		AccessChild accessClazzFields = new AccessChild(new JavaReferenceType(fieldArr), derefClazz, declaredFieldsVar, location);

		HeapDereference derefArr = new HeapDereference(fieldArr, accessClazzFields, location);

		AccessChild accessLen = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		boolean outOfBoundsFieldArr = false;
		int i = 0;

		// look for a field with the same name
		while (outOfBoundsFieldArr == false) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				idx, accessLen, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsFieldArr = true;
				break;
			}

			AccessChild accessIdx = new AccessChild(refFieldMetaType, derefArr, idx, getLocation());
			HeapDereference derefField = new HeapDereference(fieldMetaType, accessIdx, location);

			AccessChild accessName = new AccessChild(refStringType, derefField, nameVar, location);
			HeapDereference derefName = new HeapDereference(stringType, accessName, location);
			AccessChild accessValue = new AccessChild(stringType, derefName, valueVar, location);

			it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
					getProgram().getTypes().getBooleanType(),
					accessValue,
					accessFieldNameExpr,
					JavaStringEqualsOperator.INSTANCE,
					getLocation());

			Satisfiability match = analysis.satisfies(state, equalsExpr, this);

			if (match == Satisfiability.SATISFIED) {
				HeapReference refField = new HeapReference(refFieldMetaType, accessIdx, getLocation());
				AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, refField, this);
				return noExceptionState;
			}
			else if (match == Satisfiability.UNKNOWN) {
				HeapReference refField = new HeapReference(refFieldMetaType, accessIdx, getLocation());
				AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, refField, this);

				AnalysisState<A> exceptionState = throwNoSuchFieldException(interprocedural, state, expressions);

				state = noExceptionState.lub(exceptionState);
			}

			++i;
		}

		// try to look in superclasses first
		AccessChild superClass = new AccessChild(refClassMetaType, derefClazz, superClassVar, location);

		// TODO: look in interfaces too.
		// we aren't doing that since static fields in interfaces are not allowed as of now

		return searchField(interprocedural, state, superClass, right, expressions);
	}


	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNoSuchFieldException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		JavaClassType noSuchFieldType = JavaClassType.getNoSuchFieldException();

		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				noSuchFieldType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		// assign exception to variable thrower
		CFGThrow throwVar = new CFGThrow(getCFG(), noSuchFieldType.getReference(), getLocation());
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		// deletes the receiver of the constructor
		// and all the metavariables from subexpressions
		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(noSuchFieldType.getReference(), originating), this);
	}

}
