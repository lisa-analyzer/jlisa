package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
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
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

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

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);
		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", location);

		HeapDereference derefField = new HeapDereference(fieldMetaType, left, location);
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

		Set<it.unive.lisa.symbolic.value.BinaryExpression> fieldNameConstraints = getConstraints(analysis, state, accessFieldNameValue);
		Set<it.unive.lisa.symbolic.value.BinaryExpression> clazzNameConstraints = getConstraints(analysis, state, accessClazzNameValue);

		AnalysisState<A> result = state.bottomExecution();

		for (it.unive.lisa.symbolic.value.BinaryExpression clazzNameConstraint : clazzNameConstraints) {

			String clazzName = (String) ((Constant)clazzNameConstraint.getLeft()).getValue();
			clazzName = clazzName.replace('$', '.');
			Unit clazzUnit = getProgram().getUnit(clazzName);

			for (it.unive.lisa.symbolic.value.BinaryExpression fieldNameConstraint : fieldNameConstraints) {

				String fieldName = (String) ((Constant)fieldNameConstraint.getLeft()).getValue();

				Global reflectedGlobal;
				if (clazzUnit instanceof ClassUnit cu) {
				    reflectedGlobal = cu.getInstanceGlobal(fieldName, false);
				}
				else if (clazzUnit instanceof InterfaceUnit iu) {
				    reflectedGlobal = iu.getGlobal(fieldName);
				}
				else {
				    return state.topExecution();
				}

				Type reflectedFieldType = reflectedGlobal.getStaticType();

				if (reflectedGlobal.isInstance()) {

					// instance field
					GlobalVariable fieldVar = new GlobalVariable(Untyped.INSTANCE, fieldName, location);

					// safety: the cast is safe since the targetType is always a subclass of Object
					JavaReferenceType targetType = (JavaReferenceType) getRight().getStaticType();

					HeapDereference derefTarget = new HeapDereference(targetType.getInnerType(), right, location);

					SymbolicExpression access = new AccessChild(reflectedFieldType, derefTarget, fieldVar, location);

					if (reflectedFieldType.isReferenceType()) {
						access = new HeapReference(reflectedFieldType, access, location);
					}

					AnalysisState<A> t = analysis.smallStepSemantics(state, access, this);

					// TODO: box if field is a primitive type

					// JavaAssignment assign = new JavaAssignment(getCFG(), location, getRight(), getRight());
					//
					// AnalysisState<A> t = assign.fwdBinarySemantics(interprocedural, state, access, right, expressions);

					result = result.lub(t);
				}
				else {
					// static field, can either be in a class or in an interface

					// you can do reflectedGlobal.toSymbolicVariable()

					// TODO: the static variable is just a global variable with the Class name in front and '::' as separator
				}

			}
		}

		return result;

		//
		//
		// // dereference the Field meta-object: (*field)
		// HeapDereference derefField = new HeapDereference(fieldMetaType, left, loc);
		//
		// // (*field)->clazz  (reference to Class meta-object)
		// GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", loc);
		// AccessChild accessClazzRef = new AccessChild(new JavaReferenceType(classMetaType), derefField, clazzVar, loc);
		//
		// // (*(*field)->clazz)
		// HeapDereference derefClazz = new HeapDereference(classMetaType, accessClazzRef, loc);
		//
		// // (*(*field)->clazz)->name  (actual class name string)
		// GlobalVariable clazzNameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
		// AccessChild accessClazzName = new AccessChild(stringType, derefClazz, clazzNameVar, loc);
		//
		// // (*field)->name  (reference to String object)
		// GlobalVariable fieldNameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
		// AccessChild accessFieldNameRef = new AccessChild(new JavaReferenceType(stringType), derefField, fieldNameVar,
		// 		loc);
		//
		// // (*(*field)->name)->value  (actual field name constant)
		// HeapDereference derefFieldName = new HeapDereference(stringType, accessFieldNameRef, loc);
		// GlobalVariable fieldValueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);
		// AccessChild accessFieldName = new AccessChild(stringType, derefFieldName, fieldValueVar, loc);
		//
		// it.unive.lisa.symbolic.value.BinaryExpression isFieldDefined = new it.unive.lisa.symbolic.value.BinaryExpression(
		// 		stringType,
		// 		accessClazzName,
		// 		accessFieldName,
		// 		JavaIsFieldDefinedOperator.INSTANCE,
		// 		loc);
		//
		// // Avoid stale cache values when resolution is unknown.
		// ReflectionCache.lastField = null;
		//
		// // force domain to evaluate the predicate so that ReflectionCache gets populated
		// Satisfiability sat = analysis.satisfies(state, isFieldDefined, originating);
		//
		// // if predicate unsat, we cannot resolve the field here
		// if (sat == Satisfiability.NOT_SATISFIED)
		// 	return state.topExecution();
		//
		// Global field = ReflectionCache.lastField;
		//
		// if (field == null)
		// 	return state.topExecution();
		//
		// if (field.isInstance()) {
		// 	HeapDereference container = new HeapDereference(objectType, right, loc);
		// 	GlobalVariable var = field.toSymbolicVariable(loc);
		// 	AccessChild access = new AccessChild(field.getStaticType(), container, var, loc);
		// 	if (field.getStaticType().isPointerType())
		// 		return analysis.smallStepSemantics(state, new HeapReference(field.getStaticType(), access, loc), this);
		// 	return analysis.smallStepSemantics(state, access, this);
		// }
		//
		// GlobalVariable access = field.toSymbolicVariable(loc);
		// if (field.getStaticType().isPointerType())
		// 	return analysis.smallStepSemantics(state, new HeapReference(field.getStaticType(), access, loc), this);
		// return analysis.smallStepSemantics(state, access, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<it.unive.lisa.symbolic.value.BinaryExpression> getConstraints(Analysis<A, D> analysis,
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
}
