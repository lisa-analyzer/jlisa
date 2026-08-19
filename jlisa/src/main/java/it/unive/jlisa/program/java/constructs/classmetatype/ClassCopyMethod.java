package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ClassCopyMethod extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {

	protected Statement originating;

	public ClassCopyMethod(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "copyMethod", JavaClassType.getClassMetaType(), expr);
	}

	public static ClassCopyMethod build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassCopyMethod(cfg, location, params[0]);
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

		Type intType = JavaIntType.INSTANCE;
		JavaReferenceType refStringType = new JavaReferenceType(JavaClassType.getStringType());
		Type classMetaType = JavaClassType.getClassMetaType();
		JavaReferenceType refClassMetaType = new JavaReferenceType(classMetaType);
		JavaArrayType classArrType = JavaArrayType.lookup(refClassMetaType, 1);
		JavaReferenceType refClassArrType = new JavaReferenceType(classArrType);
		Type methodMetaType = JavaClassType.getMethodType();
		JavaReferenceType refMethodMetaType = new JavaReferenceType(methodMetaType);

		GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", location);
		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable typeVar = new GlobalVariable(Untyped.INSTANCE, "returnType", location);
		GlobalVariable modifiersVar = new GlobalVariable(Untyped.INSTANCE, "modifiers", location);
		GlobalVariable paramTypesVar = new GlobalVariable(Untyped.INSTANCE, "parameterTypes", location);

		AnalysisState<A> result = state.bottomExecution();

		// allocate a new Method
		MemoryAllocation created = new MemoryAllocation(methodMetaType, getLocation(), false);
		HeapReference ref = new HeapReference(refMethodMetaType, created, location);

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver method = new InstrumentedReceiver(refMethodMetaType, false, getLocation());
		AnalysisState<A> methodAllocated = analysis.assign(allocated, method, ref, this);

		HeapDereference derefThisMethod = new HeapDereference(methodMetaType, method, location);
		HeapDereference derefOther = new HeapDereference(methodMetaType, expr, location);

		// shallow copy clazz
		result = copyField(analysis, methodAllocated, derefOther, derefThisMethod, clazzVar, refClassMetaType,
				expressions);

		// shallow copy name
		result = result.lub(
				copyField(analysis, methodAllocated, derefOther, derefThisMethod, nameVar, refStringType, expressions));

		// shallow copy return type
		result = result.lub(copyField(analysis, methodAllocated, derefOther, derefThisMethod, typeVar, refClassMetaType,
				expressions));

		// shallow copy parameter types
		result = result.lub(copyField(analysis, methodAllocated, derefOther, derefThisMethod, paramTypesVar,
				refClassArrType, expressions));

		// copy modifiers
		result = result.lub(
				copyField(analysis, methodAllocated, derefOther, derefThisMethod, modifiersVar, intType, expressions));

		return result.forgetIdentifier(method, this).withExecutionExpression(ref);
	}

	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> copyField(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			HeapDereference source,
			HeapDereference dst,
			GlobalVariable var,
			Type t,
			StatementStore<A> expressions)
			throws SemanticException {

		SymbolicExpression ref = new AccessChild(t, source, var, getLocation());
		if (t instanceof JavaReferenceType)
			ref = new HeapReference(t, ref, getLocation());

		AccessChild accessDst = new AccessChild(t, dst, var, getLocation());

		AnalysisState<A> tmp = analysis.assign(state, accessDst, ref, this);
		return tmp;
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

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
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

}
