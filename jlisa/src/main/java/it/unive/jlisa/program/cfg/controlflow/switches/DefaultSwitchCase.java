package it.unive.jlisa.program.cfg.controlflow.switches;

import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchDefault;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Set;

/**
 * A default switch-case control flow structure.
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class DefaultSwitchCase {

	private final Collection<Statement> body;

	private final SwitchDefault entry;

	/**
	 * Builds the switch-case control flow structure.
	 * 
	 * @param entry the first node of {@code body}
	 * @param body  the body associated with this switch-case
	 */
	public DefaultSwitchCase(
			SwitchDefault entry,
			Collection<Statement> body) {
		this.body = body;
		this.entry = entry;
	}

	/**
	 * Yields the body of this case.
	 * 
	 * @return the body
	 */
	public Collection<Statement> getBody() {
		return body;
	}

	/**
	 * Yields the entry of the default switch case, i.e., the first node of its
	 * body.
	 * 
	 * @return the entry of the default switch case, i.e., the first node of its
	 *             body
	 */
	public SwitchDefault getEntry() {
		return entry;
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
		return "default case";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultSwitchCase other = (DefaultSwitchCase) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		return true;
	}
}
