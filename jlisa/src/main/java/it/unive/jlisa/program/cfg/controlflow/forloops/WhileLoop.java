package it.unive.jlisa.program.cfg.controlflow.forloops;

import java.util.Collection;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

/**
 * Traditional While loop
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class WhileLoop extends Loop {

	/**
	 * Builds the construct.
	 *
	 * @param cfgMatrix     the matrix of the cfg containing this structure
	 * @param condition     the condition of the loop (typically instrumented)
	 * @param collection     the collection involved in the loop
	 * @param firstFollower the first statement after the structure exits
	 * @param body          the statements in the loop body
	 */
	public WhileLoop(NodeList<CFG, Statement, Edge> cfgMatrix, Statement condition, Statement firstFollower,
			Collection<Statement> body) {
		super(cfgMatrix, condition, firstFollower, body);
	}

}
