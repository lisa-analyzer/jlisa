package it.unive.jlisa.program.language.resolution;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;
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
			for (SymbolicExpression exp : parameters[i])
				if (JavaClassType.isWrapperOf(formals[i].getStaticType(), exp.getStaticType())) {
					// boxing
					JavaNewObj wrap = new JavaNewObj(
							call.getCFG(),
							call.getLocation(),
							(JavaReferenceType) formals[i].getStaticType(),
							new Expression[] { call.getSubExpressions()[i] });
					AnalysisState<A> wrapState = wrap.forwardSemantics(prepared, interprocedural, expressions);
					for (SymbolicExpression wrapExp : wrapState.getExecutionExpressions())
						temp = temp.lub(
								interprocedural.getAnalysis().assign(
										wrapState,
										formals[i].toSymbolicVariable(),
										wrapExp,
										call)
										.forgetIdentifiers(wrap.getMetaVariables(), call));
					pars[i] = wrapState.getExecutionExpressions();
					boxingUnboxingNeeded = true;
				} else if (JavaClassType.isWrapperOf(exp.getStaticType(), formals[i].getStaticType())) {
					// unboxing
					GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", call.getLocation());
					HeapDereference derefRight = new HeapDereference(
							exp.getStaticType().asReferenceType().getInnerType(), exp, call.getLocation());
					AccessChild rightExpr = new AccessChild(formals[i].getStaticType(), derefRight, var,
							call.getLocation());
					temp = temp.lub(interprocedural.getAnalysis().assign(prepared, formals[i].toSymbolicVariable(),
							rightExpr, call));
					pars[i] = new ExpressionSet(rightExpr);
					boxingUnboxingNeeded = true;
				} else
					temp = temp.lub(
							interprocedural.getAnalysis().assign(
									prepared,
									formals[i].toSymbolicVariable(),
									exp,
									call));
			prepared = temp;
		}

		// we remove expressions from the stack
		prepared = prepared.withExecutionExpressions(new ExpressionSet());
		return Pair.of(prepared, boxingUnboxingNeeded ? pars : parameters);
	}

}
