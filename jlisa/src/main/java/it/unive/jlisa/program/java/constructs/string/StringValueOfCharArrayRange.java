package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.java.constructs.CharArrayConstantSupport;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringValueOfCharArrayRange extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringValueOfCharArrayRange(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "valueOf", left, middle, right);
	}

	public static StringValueOfCharArrayRange build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringValueOfCharArrayRange(cfg, location, params[0], params[1], params[2]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		String constantValue;
		try {
			Object offsetVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, middle, this);
			Object countVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, right, this);
			constantValue = offsetVal instanceof Integer && countVal instanceof Integer
					? CharArrayConstantSupport.computeConstantSubstring(interprocedural, state, left,
							(Integer) offsetVal, (Integer) countVal, getLocation(), this)
					: null;
		} catch (SemanticException e) {
			throw e;
		} catch (RuntimeException e) {
			// best-effort constant reconstruction: any failure here (e.g. the
			// heap not exposing a resolvable constant for some cell) must not
			// turn the whole statement's outcome into bottom, we just fall
			// back to the imprecise (top) result
			constantValue = null;
		}

		SymbolicExpression value = constantValue != null
				? new Constant(stringType, constantValue, getLocation())
				: new PushAny(stringType, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		// String.valueOf(char[], int, int) allocates and returns a NEW String
		// object: the char array passed in must not be touched
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				expressions);

		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callState, access, value, originating);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return tmp.withExecutionExpressions(callState.getExecutionExpressions());
	}
}
