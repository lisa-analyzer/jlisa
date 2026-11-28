package it.unive.jlisa.springed.security.p1.securitychains;

import java.util.List;

public class Decision {

	private final DecisionType type;
	private final List<String> values;
	private final boolean negated;

	public Decision(
			DecisionType type,
			List<String> values,
			boolean negated) {
		this.type = type;
		this.values = values;
		this.negated = negated;
	}

	public boolean isNegated() {
		return negated;
	}

	public List<String> getValues() {
		return values;
	}

	public DecisionType getType() {
		return type;
	}
}
