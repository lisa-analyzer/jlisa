package it.unive.jlisa.program.cfg.controlflow.switches;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;

/**
 * A switch control flow structure.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class Switch extends ControlFlowStructure {

	private final SwitchCase[] cases;

	private final DefaultSwitchCase defaultCase;

	/**
	 * Builds the switch control flow structure.
	 * 
	 * @param cfgMatrix     the matrix behind this control flow structure
	 * @param condition     the switch condition
	 * @param firstFollower the first follower of this control flow structure
	 * @param cases         the cases associated with this control flow
	 *                          structure
	 * @param defaultCase   the default case
	 */
	public Switch(NodeList<CFG, Statement, Edge> cfgMatrix, Statement condition, Statement firstFollower,
			SwitchCase[] cases, DefaultSwitchCase defaultCase) {
		super(cfgMatrix, condition, firstFollower);
		this.cases = cases;
		this.defaultCase = defaultCase;
	}

	@Override
	protected Collection<Statement> bodyStatements() {
		Collection<Statement> body = new HashSet<>();
		for (SwitchCase case_ : cases)
			body.addAll(case_.getBody());
		if (defaultCase != null)
			body.addAll(defaultCase.getBody());
		return body;
	}

	@Override
	public boolean contains(Statement st) {
		return bodyStatements().contains(st);
	}

	@Override
	public void simplify() {
		for (SwitchCase case_ : cases)
			case_.simplify();
		if (defaultCase != null)
			defaultCase.simplify();
	}

	@Override
	public String toString() {
		return "switch[" + getCondition() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((cases == null) ? 0 : cases.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Switch other = (Switch) obj;
		if (cases == null) {
			if (other.cases != null)
				return false;
		} else if (!cases.equals(other.cases))
			return false;
		return true;
	}

	@Override
	public Collection<Statement> getTargetedStatements() {
		Collection<Statement> targeted = new HashSet<>(cfgMatrix.followersOf(getCondition()));
		for (SwitchCase case_ : cases)
			targeted.add(case_.getCondition());
		if (defaultCase != null)
			targeted.add(defaultCase.getEntry());
		targeted.add(getFirstFollower());
		return targeted;
	}
}