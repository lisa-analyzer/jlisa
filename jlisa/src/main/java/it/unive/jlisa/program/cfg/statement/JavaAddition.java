package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
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
				|| rightType.isStringType())) {
			return Untyped.INSTANCE;
		}
		if (leftType.isStringType() || rightType.isStringType()) {
			return StringType.INSTANCE;
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
					UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), CallType.INSTANCE, null, "concat",
							lType, getSubExpressions());
					ExpressionSet[] expressionSet = new ExpressionSet[2];
					expressionSet[0] = new ExpressionSet(actualLeft);
					expressionSet[1] = new ExpressionSet(actualRight);
					partialResult = call.forwardSemanticsAux(interprocedural, state, expressionSet, expressions);
					getMetaVariables().addAll(call.getMetaVariables());
				} else if (lType.isStringType()) {
					// TODO: call to String.valueOf
					op = StringConcat.INSTANCE;
					Constant typeCast = new Constant(new TypeTokenType(Collections.singleton(StringType.INSTANCE)),
							StringType.INSTANCE, this.getLocation());
					actualRight = new BinaryExpression(getStaticType(), right, typeCast, TypeConv.INSTANCE,
							this.getLocation());
					type = StringType.INSTANCE;
					partialResult = analysis.smallStepSemantics(
							state,
							new BinaryExpression(
									type,
									actualLeft,
									actualRight,
									op,
									getLocation()),
							this);
				} else if (rType.isStringType()) {
					// TODO: call to String.valueOf
					op = StringConcat.INSTANCE;
					Constant typeCast = new Constant(new TypeTokenType(Collections.singleton(StringType.INSTANCE)),
							StringType.INSTANCE, this.getLocation());
					actualLeft = new BinaryExpression(getStaticType(), left, typeCast, TypeConv.INSTANCE,
							this.getLocation());
					type = StringType.INSTANCE;
					partialResult = analysis.smallStepSemantics(
							state,
							new BinaryExpression(
									type,
									actualLeft,
									actualRight,
									op,
									getLocation()),
							this);
				} else if (lType.isNumericType() && rType.isNumericType()) {
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
}
