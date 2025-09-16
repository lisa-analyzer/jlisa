package it.unive.jlisa.program.java.constructs.urldecoder;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class Decode extends it.unive.lisa.program.cfg.statement.BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public Decode(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "decode", left, right);
	}

	public static Decode build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new Decode(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		// as no-exception state, we return the top string
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(stringType);
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), "String", reftype,
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callState, access, new PushAny(stringType, getLocation()), this);
			tmp = tmp.lub(sem);
		}

		AnalysisState<A> noExceptionState = tmp;

		// builds the exception
		JavaClassType oobExc = JavaClassType.getUnsupportedEncodingExceptionType();
		call = new JavaNewObj(getCFG(), getLocation(), "ArrayIndexOutOfBoundsException",
				oobExc.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				new StatementStore<A>(state));

		AnalysisState<A> exceptionState = state.bottomExecution();

		for (SymbolicExpression th : state.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), oobExc.getReference(), getLocation());
			tmp = analysis.assign(state, throwVar, th, this);

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
