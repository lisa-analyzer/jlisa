package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.java.constructs.string.StringValueOf;
import it.unive.jlisa.program.type.*;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaAddition extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	public JavaAddition(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "+", inferType(left, right), left, right);
	}

	private static Type inferType(
			Expression left,
			Expression right) {
		Type leftType = left.getStaticType();
		Type rightType = right.getStaticType();

		if (!(leftType.isNumericType() || leftType.isStringType() || rightType.isNumericType()
				|| rightType.isStringType()
				|| (leftType instanceof JavaReferenceType lRef && lRef.getInnerType().isStringType()) ||
				(rightType instanceof JavaReferenceType rRef && rRef.getInnerType().isStringType()))) {
			return Untyped.INSTANCE;
		}
		if (leftType.isStringType() || rightType.isStringType()
				|| (leftType instanceof JavaReferenceType lRef && lRef.getInnerType().isStringType()) ||
				(rightType instanceof JavaReferenceType rRef && rRef.getInnerType().isStringType())) {
			return new ReferenceType(JavaClassType.lookup("java.lang.String"));
		}
		if (leftType.isNumericType() && rightType.isNumericType()) {
			// small types promoted to int for addition operation
			if (leftType instanceof JavaByteType || leftType instanceof JavaShortType) {
				leftType = JavaIntType.INSTANCE;
			}
			if (rightType instanceof JavaByteType || rightType instanceof JavaShortType) {
				rightType = JavaIntType.INSTANCE;
			}
			return leftType.commonSupertype(rightType);
		}

		return Untyped.INSTANCE;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Set<Type> leftTypes = analysis.getRuntimeTypesOf(state, left, this);
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, this);
		SymbolicExpression actualLeft = left;
		SymbolicExpression actualRight = right;
		AnalysisState<A> result = state.bottomExecution();
		BinaryOperator op;
		Type type;

		AnalysisState<A> partialResult = state.bottomExecution();
		for (Type lType : leftTypes) {
			for (Type rType : rightTypes) {
				if (lType.isReferenceType() && rType.isReferenceType()
						&& lType.asReferenceType().getInnerType().equals(JavaClassType.getStringType())
						&& lType.asReferenceType().getInnerType().equals(rType.asReferenceType().getInnerType())) {
					partialResult = concatStrings(interprocedural, state, actualLeft, actualRight, expressions);
				} else if (lType.isStringType()
						|| (lType instanceof JavaReferenceType lRef && lRef.getInnerType().isStringType())) {

					AnalysisState<A> tmp = state.bottomExecution();
					// if primitive, call String::valueOf, else call toString
					if (JavaTypeSystem.isPrimitive(rType)) {
						StringValueOf strValueOf = new StringValueOf(getCFG(), getRight().getLocation(), getRight());
						ExpressionSet param = new ExpressionSet(actualRight);
						tmp = strValueOf.forwardSemanticsAux(interprocedural, state,
								new ExpressionSet[] {param}, expressions);
					}
					else {
						UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), CallType.INSTANCE, null, "toString",
								rType, getRight());
						ExpressionSet[] args = new ExpressionSet[1];
						args[0] = new ExpressionSet(actualRight);
						tmp = call.forwardSemanticsAux(interprocedural, state, args, expressions);
						getMetaVariables().addAll(call.getMetaVariables());
					}

					for (SymbolicExpression rightStr : tmp.getExecutionExpressions()) {
						partialResult = partialResult.lub(concatStrings(interprocedural, tmp, actualLeft, rightStr, expressions));
						partialResult = partialResult.forgetIdentifier((Identifier) rightStr, this);
					}

				} else if (rType.isStringType()) {
					// TODO: call to String.valueOf
					op = StringConcat.INSTANCE;
					Constant typeCast = new Constant(
							new TypeTokenType(Collections.singleton(JavaClassType.getStringType())),
							JavaClassType.getStringType(), this.getLocation());
					actualLeft = new BinaryExpression(getStaticType(), left, typeCast, TypeConv.INSTANCE,
							this.getLocation());
					type = JavaClassType.getStringType();
					partialResult = analysis.smallStepSemantics(
							state,
							new BinaryExpression(
									type,
									actualLeft,
									actualRight,
									op,
									getLocation()),
							this);
				} else if ((lType.isReferenceType() || lType.isNumericType()) && (rType.isReferenceType() || rType.isNumericType())) {

					GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

					// left is a wrapper type
					if (lType.isReferenceType() && JavaClassType.isWrapperType(lType.asReferenceType().getInnerType())) {
						lType = lType.asReferenceType().getInnerType();
						HeapDereference derefLeft = new HeapDereference(lType, actualLeft, getLocation());
						actualLeft = new AccessChild(JavaClassType.getUnwrappedType(lType), derefLeft, valueVar, getLocation());
					}
					// left is a reference type but not a wrapper type, do nothing
					else if (lType.isReferenceType()) {
						continue;
					}

					// right is a wrapper type
					if (rType.isReferenceType() && JavaClassType.isWrapperType(rType.asReferenceType().getInnerType())) {
						rType = rType.asReferenceType().getInnerType();
						HeapDereference derefRight = new HeapDereference(rType, actualRight, getLocation());
						actualRight = new AccessChild(JavaClassType.getUnwrappedType(rType), derefRight, valueVar, getLocation());
					}
					// right is a reference type but not a wrapper type, do nothing
					else if (rType.isReferenceType()) {
						continue;
					}

					op = NumericNonOverflowingAdd.INSTANCE;
					type = lType.commonSupertype(rType);
					partialResult = analysis.smallStepSemantics(
							state,
							new BinaryExpression(
									type,
									actualLeft,
									actualRight,
									op,
									getLocation()),
							this);
				} else {
					continue;
				}

				result = result.lub(partialResult);
			}
		}

		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}


	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> concatStrings(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					// Type leftType,
					StatementStore<A> expressions)
					throws SemanticException {

		AnalysisState<A> result = state.bottomExecution();

		it.unive.jlisa.program.java.constructs.string.StringConcat strConcat = new it.unive.jlisa.program.java.constructs.string.StringConcat(getCFG(), getLocation(), getLeft(), getRight());

		result = strConcat.fwdBinarySemantics(interprocedural, state, left, right, expressions);

		return result;
	}


}
