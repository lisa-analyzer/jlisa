package it.unive.jlisa.program.java.constructs.urldecoder;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.operator.JavaIsValidEncoding;
import it.unive.jlisa.program.operator.JavaURLDecoderIsIllegalArg;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
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

		Satisfiability satIsValidEncoding = analysis.satisfies(state, isValidEncoding, originating);
		if (satIsValidEncoding == Satisfiability.BOTTOM) {
			return state.bottomExecution();
		}

		it.unive.lisa.symbolic.value.UnaryExpression isIllegalArgument = new it.unive.lisa.symbolic.value.UnaryExpression(
				booleanType,
				accessLeft,
				JavaURLDecoderIsIllegalArg.INSTANCE,
				location);

		Satisfiability satIsIllegalArg = analysis.satisfies(state, isIllegalArgument, originating);

		AnalysisState<A> noExceptionState = state.bottomExecution();
		AnalysisState<A> exceptionState = state.bottomExecution();

		// as no-exception state, we return the top string...
		JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(stringType);
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(), reftype,
				new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callState, access, new PushAny(stringType, getLocation()), this);
			tmp = tmp.lub(sem.withExecutionExpressions(callState.getExecutionExpressions()));
		}

		noExceptionState = tmp;

		if (satIsValidEncoding != Satisfiability.SATISFIED) {

			// builds the UnsupportedEncoding exception
			JavaClassType ueExc = JavaClassType.getUnsupportedEncodingExceptionType();
			call = new JavaNewObj(getCFG(), getLocation(),
					ueExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					new StatementStore<A>(state));

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), ueExc.getReference(), getLocation());
				tmp = analysis.assign(state, throwVar, th, originating);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(ueExc.getReference(), originating), this));
			}
		}

		if (satIsIllegalArg != Satisfiability.SATISFIED) {

			// builds the IllegalArgument exception
			JavaClassType iaExc = JavaClassType.getIllegalArgumentExceptionType();
			call = new JavaNewObj(getCFG(), getLocation(),
					iaExc.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					new StatementStore<A>(state));

			for (SymbolicExpression th : state.getExecutionExpressions()) {
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), iaExc.getReference(), getLocation());
				tmp = analysis.assign(state, throwVar, th, originating);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
						.forgetIdentifiers(getLeft().getMetaVariables(), this)
						.forgetIdentifiers(getRight().getMetaVariables(), this);
				exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
						new Error(iaExc.getReference(), originating), this));
			}
		}

		return noExceptionState.lub(exceptionState);
	}
}
