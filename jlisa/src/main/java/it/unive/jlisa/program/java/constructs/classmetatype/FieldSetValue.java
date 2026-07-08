package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.frontend.InitializedClassSet;
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
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.ClassUnit;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

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

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);
		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", loc);

		HeapDereference derefField = new HeapDereference(fieldMetaType, left, loc);
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

		Set<BinaryExpression> fieldNameConstraints = getConstraints(analysis, state, accessFieldNameValue);
		Set<BinaryExpression> clazzNameConstraints = getConstraints(analysis, state, accessClazzNameValue);

		AnalysisState<A> result = state.bottomExecution();

		for (BinaryExpression clazzNameConstraint : clazzNameConstraints) {

			String clazzName = (String) ((Constant) clazzNameConstraint.getLeft()).getValue();
			clazzName = clazzName.replace('$', '.');
			Unit clazzUnit = getProgram().getUnit(clazzName);

			for (BinaryExpression fieldNameConstraint : fieldNameConstraints) {

				String fieldName = (String) ((Constant) fieldNameConstraint.getLeft()).getValue();

				Global reflectedGlobal;
				if (clazzUnit instanceof ClassUnit cu) {
					reflectedGlobal = cu.getInstanceGlobal(fieldName, false);
					if (reflectedGlobal == null)
						reflectedGlobal = cu.getGlobal(fieldName);
				}
				else if (clazzUnit instanceof InterfaceUnit iu)
					reflectedGlobal = iu.getGlobal(fieldName);
				else
					return state.topExecution();

				if (reflectedGlobal == null)
					return state.topExecution();

				Type reflectedFieldType = reflectedGlobal.getStaticType();

				if (reflectedGlobal.isInstance()) {
					GlobalVariable fieldVar = new GlobalVariable(Untyped.INSTANCE, fieldName, loc);

					// safety: middle is always a subclass of Object
					JavaReferenceType targetType = (JavaReferenceType) getMiddle().getStaticType();

					HeapDereference derefTarget = new HeapDereference(targetType.getInnerType(), middle, loc);
					AccessChild access = new AccessChild(reflectedFieldType, derefTarget, fieldVar, loc);

					// NOTE: this getMiddle() is wrong, but shouldn't hurt anything. It should be a fieldAccess expression
					JavaAssignment assign = new JavaAssignment(getCFG(), loc, getMiddle(), getRight());

					AnalysisState<A> t = assign.fwdBinarySemantics(interprocedural, state, access, right, expressions);
					result = result.lub(t);
				} else {
					GlobalVariable reflectedAccess = new GlobalVariable(
							reflectedGlobal.getStaticType(),
							reflectedGlobal.getContainer().getName() + "::" + reflectedGlobal.getName(),
							reflectedGlobal.getAnnotations(),
							loc);
					VariableRef target = new VariableRef(getCFG(), loc, reflectedAccess.getName(), reflectedGlobal.getStaticType());
					JavaAssignment assign = new JavaAssignment(getCFG(), loc, target, getRight());

					AnalysisState<A> t = assign.fwdBinarySemantics(
							interprocedural,
							state,
							reflectedAccess,
							right,
							expressions);
					result = result.lub(t);
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<BinaryExpression> getConstraints(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expr) {

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

			ValueExpression ex = (ValueExpression) analysis.rewrite(state, expr, this).iterator().next();

			constraints = vdom.constraints(null, env, ex, this, oracle);
		} catch (Exception e) {
		}

		return constraints;
	}
}
