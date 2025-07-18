package it.unive.jlisa.program.cfg.controlflow.loops;

import java.util.Collection;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

/**
 * For-each loop
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class ForEachLoop extends Loop {
	
	private final Statement item;
	private final Statement collection;
	
	/**
	 * Builds the construct.
	 *
	 * @param cfgMatrix     the matrix of the cfg containing this structure
	 * @param item  the item get in each iteration
	 * @param condition     the condition of the loop (typically instrumented)
	 * @param collection     the collection involved in the loop
	 * @param firstFollower the first statement after the structure exits
	 * @param body          the statements in the loop body
	 */
	public ForEachLoop(NodeList<CFG, Statement, Edge> cfgMatrix, Statement item, Statement condition, Statement collection, Statement firstFollower,
			Collection<Statement> body) {
		super(cfgMatrix, condition, firstFollower, body);
		this.item=item;
		this.collection=collection;
	}

	public Statement getItem() {
		return item;
	}

	public Statement getCollection() {
		return collection;
	}

}
