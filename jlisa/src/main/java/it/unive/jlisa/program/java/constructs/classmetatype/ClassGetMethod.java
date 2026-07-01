package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.java.constructs.string.StringEquals;
import it.unive.jlisa.program.operator.GhostGetMethodParameterCountOperator;
import it.unive.jlisa.program.operator.JavaIsMethodDefinedOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.lang.reflect.Modifier;

public class ClassGetMethod extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public ClassGetMethod(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "getMethod", left, middle, right);
	}

	public static ClassGetMethod build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ClassGetMethod(cfg, location, params[0], params[1], params[2]);
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

		Type classMetaType = JavaClassType.getClassMetaType();
		Type methodType = JavaClassType.getMethodType();
		JavaReferenceType refMethodType = new JavaReferenceType(methodType);
		JavaArrayType methodArrType = JavaArrayType.lookup(refMethodType, 1);
		JavaReferenceType refMethodArrType = new JavaReferenceType(methodArrType);

		GlobalVariable lengthVar = new GlobalVariable(Untyped.INSTANCE, "length", location);
		GlobalVariable declaredMethodsVar = new GlobalVariable(Untyped.INSTANCE, "declaredMethods", location);

		// (*left)->declaredMethods
		HeapDereference derefClazz = new HeapDereference(classMetaType, left, location);
		AccessChild accessDeclaredMethods = new AccessChild(refMethodArrType, derefClazz, declaredMethodsVar, location);

		// *((*left)->declaredMethods)
		HeapDereference derefArr = new HeapDereference(methodArrType, accessDeclaredMethods, location);

		// (*(*left)->declaredMethods)->length
		AccessChild lenAccess = new AccessChild(JavaIntType.INSTANCE, derefArr, lengthVar, location);

		boolean outOfBoundsMethodArr = false;
		int i = 0;

		// stop when we are out of bounds
		while (outOfBoundsMethodArr == false) {

			Constant idx = new Constant(JavaIntType.INSTANCE, i, location);

			it.unive.lisa.symbolic.value.BinaryExpression withinBounds = new it.unive.lisa.symbolic.value.BinaryExpression( JavaBooleanType.INSTANCE,
				idx, lenAccess, ComparisonLt.INSTANCE, location);

			Satisfiability sat = analysis.satisfies(state, withinBounds, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				outOfBoundsMethodArr = true;
				break;
			}

			// check if the two methods' signatures are the same

			AccessChild accessMethod = new AccessChild(refMethodType, derefArr, idx, location);
			boolean methodFound = matchesTarget(interprocedural, state, accessMethod, middle, right);

			if (methodFound) {
				HeapReference refMethod = new HeapReference(refMethodType, accessMethod, location);
				return analysis.smallStepSemantics(state, refMethod, this);
			}
			++i;
		}

		// TODO AP: method not found here

		return state.bottomExecution();
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	// check whether a target method matches the signature of the candidate one
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean matchesTarget(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression candidateMethod,
			SymbolicExpression targetMethodName,
			SymbolicExpression targetMethodParameterTypes)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CodeLocation location = getLocation();

		Type stringType = JavaClassType.getStringType();
		JavaReferenceType refStringType = new JavaReferenceType(stringType);
		Type methodMetaType = JavaClassType.getMethodType();

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", location);
		GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", location);

		// candidateMethod is of type Method*

		// stringequals on the names
		HeapDereference derefMethod = new HeapDereference(methodMetaType, candidateMethod, location);
		AccessChild accessMethodName = new AccessChild(refStringType, derefMethod, nameVar, location);

		HeapDereference derefMethodName = new HeapDereference(stringType, accessMethodName, location);
		AccessChild accessMethodNameValue = new AccessChild(stringType, derefMethodName, valueVar, location);

		HeapDereference derefTargetMethodName = new HeapDereference(stringType, targetMethodName, location);
		AccessChild accessTargetMethodNameValue = new AccessChild(stringType, derefTargetMethodName, valueVar, location);

		it.unive.lisa.symbolic.value.BinaryExpression equalsExpr = new it.unive.lisa.symbolic.value.BinaryExpression(
				getProgram().getTypes().getBooleanType(),
				accessMethodNameValue,
				accessTargetMethodNameValue,
				JavaStringEqualsOperator.INSTANCE,
				getLocation());

		Satisfiability nameMatches = analysis.satisfies(state, equalsExpr, this);

		if (nameMatches == Satisfiability.NOT_SATISFIED) {
			return false;
		}

		// TODO AP: == on every single parameter type


		return true;
	}

}
