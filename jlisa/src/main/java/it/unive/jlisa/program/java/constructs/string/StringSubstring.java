package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
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
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringSubstring extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected StringSubstring(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "substring", left, right);
	}

	public static StringSubstring build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringSubstring(cfg, location, params[0], params[1]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(stringType, left, getLocation());
		AccessChild accessLeft = new AccessChild(stringType, derefLeft, var, getLocation());

		// check for IndexOutOfBoundException
		// index < 0 or index >= length
		it.unive.lisa.symbolic.value.UnaryExpression length = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE,
				accessLeft,
				JavaStringLengthOperator.INSTANCE,
				getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck1 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, new Constant(JavaIntType.INSTANCE, 0, getLocation()), ComparisonLt.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression idxCheck2 = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, length, ComparisonGe.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression or = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				idxCheck1, idxCheck2, LogicalOr.INSTANCE, getLocation());

		Satisfiability sat = analysis.satisfies(state, or, this);

		if (sat == Satisfiability.SATISFIED) {
			// builds the exception
			JavaClassType oonExc = JavaClassType.getIndexOutOfBoundsExceptionType();
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

			it.unive.lisa.symbolic.value.BinaryExpression substring = new it.unive.lisa.symbolic.value.BinaryExpression(
					getProgram().getTypes().getBooleanType(),
					accessLeft,
					right,
					JavaStringSubstringOperator.INSTANCE,
					getLocation());

			// allocate the string
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
					new JavaReferenceType(stringType), new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild access = new AccessChild(stringType, ref, var, getLocation());
				AnalysisState<A> sem = analysis.assign(callState, access, substring, this);
				tmp = tmp.lub(sem);
			}

			getMetaVariables().addAll(call.getMetaVariables());
			return tmp.withExecutionExpressions(callState.getExecutionExpressions());
		} else {

			it.unive.lisa.symbolic.value.BinaryExpression substring = new it.unive.lisa.symbolic.value.BinaryExpression(
					getProgram().getTypes().getBooleanType(),
					accessLeft,
					right,
					JavaStringSubstringOperator.INSTANCE,
					getLocation());

			// allocate the string
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), 
					new JavaReferenceType(stringType), new Expression[0]);
			AnalysisState<
					A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild access = new AccessChild(stringType, ref, var, getLocation());
				AnalysisState<A> sem = analysis.assign(callState, access, substring, this);
				tmp = tmp.lub(sem);
			}

			getMetaVariables().addAll(call.getMetaVariables());
			AnalysisState<A> noExceptionState = tmp.withExecutionExpressions(callState.getExecutionExpressions());

			// builds the exception
			JavaClassType oonExc = JavaClassType.getIndexOutOfBoundsExceptionType();
			call = new JavaNewObj(getCFG(), getLocation(), 
					oonExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
			AnalysisState<A> exceptionState = state.bottomExecution();

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), oonExc.getReference(), getLocation());
				tmp = analysis.assign(state, throwVar, th, this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(oonExc.getReference(), this)));
			}

			return exceptionState.lub(noExceptionState);
		}
	}
}
