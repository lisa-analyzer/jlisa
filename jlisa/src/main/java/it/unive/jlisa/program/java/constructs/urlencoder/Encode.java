package it.unive.jlisa.program.java.constructs.urlencoder;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaIsValidEncoding;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;

public class Encode extends it.unive.lisa.program.cfg.statement.BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public Encode(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "encode", left, right);
	}

	public static Encode build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new Encode(cfg, location, params[0], params[1]);
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

		TypeSystem typeSystem = getProgram().getTypes();
		Type booleanType = typeSystem.getBooleanType();
		Type stringType = typeSystem.getStringType();

		CodeLocation location = getLocation();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference derefLeft = new HeapDereference(stringType, left, location);
		AccessChild accessLeft = new AccessChild(stringType, derefLeft, var, location);

		HeapDereference derefRight = new HeapDereference(stringType, right, location);
		AccessChild accessRight = new AccessChild(stringType, derefRight, var, location);

		it.unive.lisa.symbolic.value.UnaryExpression isValidEncoding = new it.unive.lisa.symbolic.value.UnaryExpression(
				booleanType,
				accessRight,
				JavaIsValidEncoding.INSTANCE,
				location);

		Satisfiability sat = analysis.satisfies(state, isValidEncoding, originating);
		if (sat == Satisfiability.BOTTOM) {
			return state.bottomExecution();
		}

		AnalysisState<A> noExceptionState = state.bottomExecution();
		if (sat != Satisfiability.NOT_SATISFIED) {
			// as no-exception state, we return the top string...
			AnalysisState<A> validState = analysis.assume(state, isValidEncoding, originating, originating);
			JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(stringType);
			JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), reftype,
					new Expression[0]);
			AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, validState, new ExpressionSet[0],
					expressions);

			for (SymbolicExpression ref : callState.getExecutionExpressions()) {
				AccessChild access = new AccessChild(stringType, ref, var, getLocation());
				AnalysisState<
						A> sem = analysis.assign(callState, access, new PushAny(stringType, getLocation()), this);
				noExceptionState = noExceptionState
						.lub(sem.withExecutionExpressions(callState.getExecutionExpressions()));
			}
		}

		if (sat == Satisfiability.SATISFIED)
			return noExceptionState;

		// builds the exception
		AnalysisState<A> invalidState = analysis.assume(state,
				new it.unive.lisa.symbolic.value.UnaryExpression(booleanType, isValidEncoding, LogicalNegation.INSTANCE,
						location),
				originating, originating);
		JavaClassType oobExc = JavaClassType.getUnsupportedEncodingExceptionType();
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				oobExc.getReference(), new Expression[0]);
		AnalysisState<A> excState = call.forwardSemanticsAux(interprocedural, invalidState, new ExpressionSet[0],
				new StatementStore<A>(invalidState));

		AnalysisState<A> erroneousState = state.bottomExecution();
		for (SymbolicExpression th : excState.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), oobExc.getReference(), getLocation());
			AnalysisState<A> tmp = analysis.assign(excState, throwVar, th, originating);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
					.forgetIdentifiers(getLeft().getMetaVariables(), this)
					.forgetIdentifiers(getRight().getMetaVariables(), this);
			erroneousState = erroneousState.lub(tmp.withExecutionExpression(throwVar));
		}

		Error error = new Error(oobExc.getReference(), originating);
		if (sat == Satisfiability.NOT_SATISFIED)
			return analysis.moveExecutionToError(erroneousState, error, this);
		return analysis.moveExecutionToError(noExceptionState, erroneousState, error, this);
	}
}
