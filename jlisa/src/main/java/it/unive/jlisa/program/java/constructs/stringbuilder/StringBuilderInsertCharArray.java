package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.java.constructs.CharArrayConstantSupport;
import it.unive.jlisa.program.operator.JavaStringInsertStringOperator;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

public class StringBuilderInsertCharArray extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderInsertCharArray(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "insert", left, middle, right);
	}

	public static StringBuilderInsertCharArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderInsertCharArray(cfg, location, params[0], params[1], params[2]);
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
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		String constantValue;
		try {
			Integer length = CharArrayConstantSupport.extractArrayLength(interprocedural, state, right, getLocation(),
					this);
			constantValue = length == null
					? null
					: CharArrayConstantSupport.computeConstantSubstring(interprocedural, state, right, 0, length,
							getLocation(), this);
		} catch (SemanticException e) {
			throw e;
		} catch (RuntimeException e) {
			// best-effort constant reconstruction: any failure here must not
			// turn the whole statement's outcome into bottom, we just fall
			// back to the imprecise (top) result
			constantValue = null;
		}

		SymbolicExpression insertedValue = constantValue != null
				? new Constant(stringType, constantValue, getLocation())
				: new PushAny(stringType, getLocation());

		AnalysisState<A> result = StringBuilderMutationSupport.mutateValue(analysis, state, left, stringType,
				getLocation(), this,
				oldValue -> new it.unive.lisa.symbolic.value.TernaryExpression(
						stringType, oldValue, middle, insertedValue, JavaStringInsertStringOperator.INSTANCE,
						getLocation()));

		return analysis.smallStepSemantics(result, left, originating);
	}
}
