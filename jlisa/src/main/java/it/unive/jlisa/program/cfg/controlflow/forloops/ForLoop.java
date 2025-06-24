package it.unive.jlisa.program.cfg.controlflow.forloops;

import java.util.Collection;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

/**
 * Traditional For loop
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class ForLoop extends Loop {

	private final Collection<Statement> initializers;
	private final Collection<Statement> updaters;
	
	/**
	 * Builds the construct.
	 *
	 * @param cfgMatrix     the matrix of the cfg containing this structure
	 * @param initializers  the variable declarations initialized at the beginning of the loop
	 * @param condition     the condition of the loop
	 * @param updaters     the increments/decrements post block iteration
	 * @param firstFollower the first statement after the structure exits
	 * @param body          the statements in the loop body
	 */
	public ForLoop(NodeList<CFG, Statement, Edge> cfgMatrix, Collection<Statement> initializers, Statement condition, Collection<Statement> updaters, Statement firstFollower,
			Collection<Statement> body) {
		super(cfgMatrix, condition, firstFollower, body);
		this.initializers = initializers;
		this.updaters=updaters;
	}

	public Collection<Statement> getInitializers() {
		return initializers;
	}

	public Collection<Statement> getUpdaters() {
		return updaters;
	}

	
}
