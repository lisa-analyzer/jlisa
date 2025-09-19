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
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.type.Untyped;

public class JavaArrayAccess extends BinaryExpression {

	public JavaArrayAccess(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "[]", left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (!left.getStaticType().isReferenceType()
				|| !left.getStaticType().asReferenceType().getInnerType().isArrayType())
			return state.bottomExecution();

		// need to check in-bound
		JavaArrayType arrayType = (JavaArrayType) ((JavaReferenceType) left.getStaticType()).getInnerType();
		HeapDereference container = new HeapDereference(arrayType, left, getLocation());
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "len", getLocation());
		AccessChild lenAccess = new AccessChild(Untyped.INSTANCE, container, lenProperty, getLocation());
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		it.unive.lisa.symbolic.value.BinaryExpression bin = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE, right, lenAccess, ComparisonGe.INSTANCE, getLocation());

		Satisfiability sat = analysis.satisfies(state, bin, this);
		if (sat == Satisfiability.SATISFIED) {
			// builds the exception
			JavaClassType oonExc = JavaClassType.getArrayIndexOutOfBoundsExceptionType();
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
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oonExc.getReference(), this)));
			}

			return exceptionState;
		} else if (sat == Satisfiability.NOT_SATISFIED) {
			AccessChild access = new AccessChild(arrayType.getInnerType(), container, right, getLocation());
			return analysis.smallStepSemantics(state, access, this);
		} else {
			AccessChild access = new AccessChild(arrayType.getInnerType(), container, right, getLocation());
			AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, access, this);

			// builds the exception
			JavaClassType oobExc = JavaClassType.getArrayIndexOutOfBoundsExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), 
					oobExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					new StatementStore<A>(state));

			AnalysisState<A> exceptionState = state.bottomExecution();

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), oobExc.getReference(), getLocation());
				AnalysisState<A> tmp = analysis.assign(state, throwVar, th, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oobExc.getReference(), this)));
			}

			return noExceptionState.lub(exceptionState);
		}
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return getLeft() + "[" + getRight() + "]";
	}
}
