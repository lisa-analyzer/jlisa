package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import java.util.Set;

public class JavaNewArray extends UnaryExpression {

	public JavaNewArray(
			CFG cfg,
			CodeLocation location,
			Expression subExpression,
			JavaReferenceType type) {
		super(cfg, location, "new", type, subExpression);
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		JavaReferenceType refType = (JavaReferenceType) getStaticType();

		// check for negative size
		it.unive.lisa.symbolic.value.BinaryExpression bin = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, expr,
				new Constant(getProgram().getTypes().getIntegerType(), 0, getLocation()), ComparisonLt.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, bin, this);
		if (sat == Satisfiability.SATISFIED) {
			// builds the exception
			JavaClassType oonExc = JavaClassType.getNegativeArraySizeExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					oonExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
			AnalysisState<A> exceptionState = state.bottomExecution();

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), oonExc.getReference(), getLocation());
				AnalysisState<A> tmp = analysis.assign(state, throwVar, th, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oonExc.getReference(), this), this));
			}

			return exceptionState;
		} else {
			// TODO: UNKNOWN case
		}

		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());

		AnalysisState<A> tmp = analysis.assign(allocated, array, ref, this);

		Type contentType = ((JavaArrayType) refType.getInnerType()).getInnerType();
		contentType = contentType.isArrayType() ? contentType.asArrayType().getInnerType() : contentType;

		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", getLocation());

		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());

		tmp = analysis.assign(tmp, lenAccess, expr, this);

		SymbolicExpression tmpExpr = expr;
		if (!(tmpExpr instanceof Constant)) {
			tmpExpr = getArrLen(interprocedural, state, expr);
		}

		// first case: the size is constant or an expression that can be
		// rewritten into just one constant value
		if (tmpExpr instanceof Constant) {
			for (int i = 0; i < (Integer) ((Constant) tmpExpr).getValue(); i++) {
				Constant var = new Constant(JavaIntType.INSTANCE, i, getLocation());
				AccessChild access = new AccessChild(contentType, array, var, getLocation());

				AnalysisState<A> tmp2 = contentType.defaultValue(getCFG(), getLocation()).forwardSemantics(tmp,
						interprocedural, expressions);
				AnalysisState<A> init = state.bottomExecution();

				for (SymbolicExpression v : tmp2.getExecutionExpressions()) {
					init = init.lub(analysis.assign(tmp2, access, v, this));
				}

				tmp = init;
			}
		}

		// FIXME: second case: the size is not constant, return the 'top' array
		// this is a temporary solution
		getMetaVariables().add(array);
		return analysis.smallStepSemantics(tmp, array, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "new " + getStaticType() + "[" + getSubExpression() + "]";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Constant getArrLen(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		if (analysis.domain instanceof SimpleAbstractDomain<?, ?, ?> innerDomain) {

			assert (innerDomain != null);
			ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

			SimpleAbstractState simpleAbstractState = (SimpleAbstractState) state.getExecutionState();

			ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

			SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

			ExpressionSet rewritten = analysis.rewrite(state, expr, this);

			if (rewritten.size() == 1) {
				ValueExpression first = (ValueExpression) rewritten.iterator().next();
				Set<BinaryExpression> constraints = vdom.constraints(null, env, first, this, oracle);

				if (constraints == null || constraints.size() != 1)
					return null;

				Constant value = (Constant) ((BinaryExpression) constraints.iterator().next()).getLeft();
				return value;
			}
		}

		return null;
	}
}
