package it.unive.jlisa.program.cfg.controlflow.loops;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Synchronized block
 * https://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.19
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class SynchronizedBlock extends ControlFlowStructure {

	private final Statement syncTarget;
	private final Collection<Statement> body;

	public SynchronizedBlock(
			NodeList<CFG, Statement, Edge> cfgMatrix,
			Statement syncTarget,
			Statement nullnessCondition,
			Collection<Statement> body,
			Statement firstFollower) {
		super(cfgMatrix, nullnessCondition, firstFollower);
		this.syncTarget = syncTarget;
		this.body = body;
	}

	@Override
	protected Collection<Statement> bodyStatements() {
		return body;
	}

	@Override
	public boolean contains(
			Statement st) {
		return body.contains(st);
	}

	@Override
	public void simplify() {
		body.removeIf(NoOp.class::isInstance);
	}

	@Override
	public String toString() {
		return "synchronized[" + syncTarget + "]";
	}

	@Override
	public Collection<Statement> getTargetedStatements() {
		Collection<Statement> targeted = new HashSet<>(cfgMatrix.followersOf(getCondition()));
		targeted.add(getCondition());
		return targeted;
	}

	@Override
	public void addWith(
			Statement toAdd,
			Statement reference) {
		if (body.contains(reference))
			body.add(toAdd);
	}

	@Override
	public void replace(
			Statement original,
			Statement replacement) {
		if (body.contains(original)) {
			body.remove(replacement);
			body.add(replacement);
		}
	}

}
