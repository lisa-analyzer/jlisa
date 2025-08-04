package it.unive.jlisa.program.cfg.controlflow.switches.instrumentations;

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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;

/**
 * Instrumentation for switch statements: Java 8, the switch apply == for literals, and equals for strings
 * It is unknown at parsing time (no reasoning on types)
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class SwitchEqualityCheck extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the break, happening at the given location in the program.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where this statement is defined within the
	 *                     program
	 */
	public SwitchEqualityCheck(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "==/equals", left, right);
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, SymbolicExpression left, SymbolicExpression right, StatementStore<A> expressions)
			throws SemanticException {
		//TODO: handle case with equals
		
		return interprocedural.getAnalysis().smallStepSemantics(
				state,
				new BinaryExpression(
						getStaticType(),
						left,
						right,
						ComparisonEq.INSTANCE,
						getLocation()),
				this); // case with ==
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

}
