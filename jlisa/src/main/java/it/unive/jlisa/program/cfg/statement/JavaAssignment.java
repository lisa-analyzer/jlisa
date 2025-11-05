package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.evaluation.RightToLeftEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaAssignment extends Assignment {

	public JavaAssignment(
			CFG cfg,
			CodeLocation location,
			Expression target,
			Expression expression) {
		super(cfg, location, RightToLeftEvaluation.INSTANCE, target.getStaticType(), target, expression);
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
		CodeLocation loc = getLocation();
		AnalysisState<A> result = state.bottomExecution();
		Type targetType = left.getStaticType();
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, this);

		// int constants, if they fit the target type, can be assigned
		if ((targetType instanceof JavaByteType || targetType instanceof JavaShortType
				|| targetType instanceof JavaCharType)
				&& right instanceof Constant c && c.getValue() instanceof Integer intVal) {

			if (isIntegerFittableInType(targetType, intVal)) {
				Constant newConst = new Constant(targetType, intVal, loc);
				return super.fwdBinarySemantics(interprocedural, state, left, newConst, expressions);
			} else
				// cannot assign: int constant
				// doesn't fit target type
				return state.bottomExecution();
		}

		for (Type rType : rightTypes) {
			if (rType.equals(left.getStaticType()) || left.getStaticType().isUntyped()) {
				SymbolicExpression lhs = left;
				if (left instanceof HeapReference)
					// assignments dereference the lhs
					lhs = ((HeapReference) left).getExpression();
				result = result.lub(super.fwdBinarySemantics(interprocedural, state, lhs, right, expressions));
			} else if (rType.canBeAssignedTo(left.getStaticType())) {
				if (left.getStaticType().isReferenceType()) { // type-cast
					Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(left.getStaticType())),
							left.getStaticType(), loc);
					BinaryExpression castExpression = new BinaryExpression(left.getStaticType(), right, typeConv,
							TypeCast.INSTANCE, loc);
					result = result
							.lub(super.fwdBinarySemantics(interprocedural, state, left, castExpression, expressions));
				} else { // type-conv
					Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(left.getStaticType())),
							left.getStaticType(), loc);
					BinaryExpression castExpression = new BinaryExpression(left.getStaticType(), right, typeConv,
							TypeConv.INSTANCE, loc);
					result = result
							.lub(super.fwdBinarySemantics(interprocedural, state, left, castExpression, expressions));
				}
			} else if (left.getStaticType().canBeAssignedTo(rType)) {
				// left is smaller that right. we do a narrowing.
				Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(left.getStaticType())),
						left.getStaticType(), loc);
				BinaryExpression castExpression = new BinaryExpression(left.getStaticType(), right, typeConv,
						TypeConv.INSTANCE, loc);
				result = result
						.lub(super.fwdBinarySemantics(interprocedural, state, left, castExpression, expressions));
			} else if (JavaClassType.isWrapperOf(left.getStaticType(), rType)) {
				// boxing
				JavaNewObj wrap = new JavaNewObj(getCFG(), this.getLocation(), (JavaReferenceType) left.getStaticType(),
						new Expression[] { getRight() });
				AnalysisState<A> wrapState = wrap.forwardSemantics(state, interprocedural, expressions);
				for (SymbolicExpression wrapExp : wrapState.getExecutionExpressions())
					result = result.lub(
							super.fwdBinarySemantics(
									interprocedural,
									wrapState,
									left,
									wrapExp,
									expressions)
											.forgetIdentifiers(wrap.getMetaVariables(), this));
			} else if (JavaClassType.isWrapperOf(rType, left.getStaticType())) {
				// unboxing
				GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
				HeapDereference derefRight = new HeapDereference(rType.asReferenceType().getInnerType(), right,
						getLocation());
				AccessChild rightExpr = new AccessChild(left.getStaticType(), derefRight, var, getLocation());
				result = result.lub(analysis.assign(state, left, rightExpr, this));
			}
		}
		return result;
	}

	/**
	 * Checks if an integer fits in a given Java type (byte, short, or char).
	 */
	private boolean isIntegerFittableInType(
			Type type,
			int value) {
		if (type instanceof JavaShortType)
			return JavaShortType.fitsInType(value);
		if (type instanceof JavaCharType)
			return JavaCharType.fitsInType(value);
		if (type instanceof JavaByteType)
			return JavaByteType.fitsInType(value);
		return false;
	}
}
