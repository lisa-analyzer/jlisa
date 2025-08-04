package it.unive.jlisa.program.cfg.expression;

import java.util.Collections;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;

public class InstanceOf extends UnaryExpression {

	private final Type type;

	public InstanceOf(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "instanceof", subExpression);
		this.type = new ReferenceType(type);
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		TypeTokenType typeToken = new TypeTokenType(Collections.singleton(type));
		BinaryExpression tc = new BinaryExpression(Untyped.INSTANCE, expr, new Constant(typeToken, 0, getLocation()), TypeCheck.INSTANCE, getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, tc, this);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "instanceof(" + getSubExpression() + "," + type + ")";
	}
}
