package it.unive.jlisa.program.cfg.expression;

import java.util.Set;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class InstanceOf extends UnaryExpression {

	private final Type type;
	
    public InstanceOf(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
        super(cfg, location, "instanceof", subExpression);
        this.type = new ReferenceType(type);
    }
    
	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		Set<Type> leftTypes = state.getState().getRuntimeTypesOf(expr, this, state.getState());

		AnalysisState<A> result = state.bottom();
		
		for (Type lType : leftTypes)
			if (lType.canBeAssignedTo(type))
				result = result.lub(state.smallStepSemantics(new Constant(BoolType.INSTANCE, true, getLocation()), this));
			else
				result = result.lub(state.smallStepSemantics(new Constant(BoolType.INSTANCE, false, getLocation()), this));

		return result;
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
