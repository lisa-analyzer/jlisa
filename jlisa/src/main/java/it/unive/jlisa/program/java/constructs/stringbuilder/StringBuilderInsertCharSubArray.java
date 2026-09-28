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
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

public class StringBuilderInsertCharSubArray extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderInsertCharSubArray(
			CFG cfg,
			CodeLocation location,
			Expression par0,
			Expression par1,
			Expression par2,
			Expression par3,
			Expression par4) {
		super(cfg, location, "insert", par0, par1, par2, par3, par4);
	}

	public static StringBuilderInsertCharSubArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderInsertCharSubArray(cfg, location, params[0], params[1], params[2], params[3],
				params[4]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		SymbolicExpression[] exprs = new SymbolicExpression[params.length];

		for (int i = 0; i < params.length; ++i) {
			ExpressionSet set = params[i];
			if (set.size() > 1 || set.size() <= 0)
				throw new IllegalArgumentException("Number of operands is incorrect!");
			for (SymbolicExpression expr : set) {
				exprs[i] = expr;
			}
		}

		SymbolicExpression left = exprs[0];
		SymbolicExpression index = exprs[1];
		SymbolicExpression arrayRef = exprs[2];
		SymbolicExpression arrayOffset = exprs[3];
		SymbolicExpression arrayLen = exprs[4];

		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		String constantValue;
		try {
			Object offsetVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, arrayOffset,
					this);
			Object lenVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, arrayLen, this);
			constantValue = offsetVal instanceof Integer && lenVal instanceof Integer
					? CharArrayConstantSupport.computeConstantSubstring(interprocedural, state, arrayRef,
							(Integer) offsetVal, (Integer) lenVal, getLocation(), this)
					: null;
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
						stringType, oldValue, index, insertedValue, JavaStringInsertStringOperator.INSTANCE,
						getLocation()));

		return analysis.smallStepSemantics(result, left, originating);
	}
}
