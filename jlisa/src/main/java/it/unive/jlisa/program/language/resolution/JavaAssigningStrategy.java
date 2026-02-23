package it.unive.jlisa.program.language.resolution;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A strategy that passes the parameters in the same order as they are
 * specified.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaAssigningStrategy
		implements
		ParameterAssigningStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaAssigningStrategy INSTANCE = new JavaAssigningStrategy();

	private JavaAssigningStrategy() {
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Pair<AnalysisState<A>, ExpressionSet[]> prepare(
			Call call,
			AnalysisState<A> callState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions,
			Parameter[] formals,
			ExpressionSet[] parameters)
			throws SemanticException {
		// prepare the state for the call: assign the value to each parameter
		AnalysisState<A> prepared = callState;
		ExpressionSet[] pars = new ExpressionSet[parameters.length];
		System.arraycopy(parameters, 0, pars, 0, pars.length);
		boolean boxingUnboxingNeeded = false;

		for (int i = 0; i < formals.length; i++) {
			AnalysisState<A> temp = prepared.bottomExecution();
			for (SymbolicExpression exp : parameters[i]) {
				Type formalType = formals[i].getStaticType();
				Variable formalVar = formals[i].toSymbolicVariable();
				Type actualType = exp.getStaticType();
				if (!formalType.isUntyped() && actualType.canBeAssignedTo(formalType) && !formalType.isReferenceType()
						&& !actualType.equals(formalType)) {
					// type-conv
					Constant typeConv = new Constant(
							new TypeTokenType(Collections.singleton(formalType)),
							formalType,
							call.getLocation());
					BinaryExpression castExpression = new BinaryExpression(
							formalType,
							exp,
							typeConv,
							TypeConv.INSTANCE,
							call.getLocation());
					temp = temp.lub(interprocedural.getAnalysis().assign(prepared, formalVar, castExpression, call));
					Set<SymbolicExpression> set = new HashSet<>(pars[i].elements());
					set.remove(exp);
					set.add(castExpression);
					pars[i] = new ExpressionSet(set);
					boxingUnboxingNeeded = true;
				} else if (JavaClassType.isWrapperOf(formalType, actualType)) {
					// boxing
					JavaNewObj wrap = new JavaNewObj(
							call.getCFG(),
							call.getLocation(),
							(JavaReferenceType) formalType,
							new Expression[] { call.getSubExpressions()[i] });
					AnalysisState<A> wrapState = wrap.forwardSemantics(prepared, interprocedural, expressions);
					for (SymbolicExpression wrapExp : wrapState.getExecutionExpressions())
						temp = temp.lub(
								interprocedural.getAnalysis().assign(
										wrapState,
										formalVar,
										wrapExp,
										call)
										.forgetIdentifiers(wrap.getMetaVariables(), call));
					Set<SymbolicExpression> set = new HashSet<>(pars[i].elements());
					set.remove(exp);
					set.addAll(wrapState.getExecutionExpressions().elements());
					pars[i] = new ExpressionSet(set);
					boxingUnboxingNeeded = true;
				} else if (JavaClassType.isWrapperOf(actualType, formalType)) {
					// unboxing
					GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", call.getLocation());
					HeapDereference derefRight = new HeapDereference(
							actualType.asReferenceType().getInnerType(), exp, call.getLocation());
					AccessChild rightExpr = new AccessChild(formalType, derefRight, var,
							call.getLocation());
					temp = temp.lub(interprocedural.getAnalysis().assign(prepared, formalVar,
							rightExpr, call));
					Set<SymbolicExpression> set = new HashSet<>(pars[i].elements());
					set.remove(exp);
					set.add(rightExpr);
					pars[i] = new ExpressionSet(set);
					boxingUnboxingNeeded = true;
				} else
					temp = temp.lub(
							interprocedural.getAnalysis().assign(
									prepared,
									formalVar,
									exp,
									call));
			}
			prepared = temp;
		}

		// we remove expressions from the stack
		prepared = prepared.withExecutionExpressions(new ExpressionSet());
		return Pair.of(prepared, boxingUnboxingNeeded ? pars : parameters);
	}

}
