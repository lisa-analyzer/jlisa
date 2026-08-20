package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.java.constructs.CharArrayConstantSupport;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringCharArrayFromToConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringCharArrayFromToConstructor(
			CFG cfg,
			CodeLocation location,
			Expression thisExpr,
			Expression val,
			Expression offset,
			Expression count) {
		super(cfg, location, "String", thisExpr, val, offset, count);
	}

	public static StringCharArrayFromToConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringCharArrayFromToConstructor(cfg, location, params[0], params[1], params[2], params[3]);
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
		SymbolicExpression arrayRef = exprs[1];
		SymbolicExpression arrayOffset = exprs[2];
		SymbolicExpression arrayCount = exprs[3];

		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild leftAccess = new AccessChild(stringType, left, var, getLocation());

		String constantValue;
		try {
			Object offsetVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, arrayOffset,
					this);
			Object countVal = CharArrayConstantSupport.extractConstantValue(interprocedural, state, arrayCount, this);
			constantValue = offsetVal instanceof Integer && countVal instanceof Integer
					? CharArrayConstantSupport.computeConstantSubstring(interprocedural, state, arrayRef,
							(Integer) offsetVal, (Integer) countVal, getLocation(), this)
					: null;
		} catch (SemanticException e) {
			throw e;
		} catch (RuntimeException e) {
			// best-effort constant reconstruction: any failure here must not
			// turn the whole statement's outcome into bottom, we just fall
			// back to the imprecise (top) result
			constantValue = null;
		}

		SymbolicExpression value = constantValue != null
				? new Constant(stringType, constantValue, getLocation())
				: new PushAny(stringType, getLocation());
		return interprocedural.getAnalysis().assign(state, leftAccess, value, originating);
	}
}
