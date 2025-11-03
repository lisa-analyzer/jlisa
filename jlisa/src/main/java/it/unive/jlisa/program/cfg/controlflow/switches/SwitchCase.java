package it.unive.jlisa.program.cfg.controlflow.switches;

import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * A switch-case control flow structure.
 *
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class SwitchCase {

	private final Expression condition;

	private final Collection<Statement> body;

	/**
	 * Builds the switch-case control flow structure.
	 *
	 * @param condition the switch-case condition
	 * @param body      the body associated with this switch-case
	 */
	public SwitchCase(
			Expression condition,
			Collection<Statement> body) {
		this.condition = condition;
		this.body = body;
	}

	/**
	 * Yields the body of this switch case.
	 *
	 * @return the body of this switch case
	 */
	public Collection<Statement> getBody() {
		return body;
	}

	/**
	 * Yields the condition associated with this switch case.
	 *
	 * @return the condition associated with this switch case
	 */
	public Expression getCondition() {
		return condition;
	}

	/**
	 * Simplifies the switch case.
	 */
	public void simplify(
			Set<Statement> targets) {
		body.removeIf(targets::contains);
	}

	@Override
	public String toString() {
		return "case[" + condition + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, condition);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SwitchCase other = (SwitchCase) obj;
		return Objects.equals(body, other.body) && Objects.equals(condition, other.condition);
	}
}
