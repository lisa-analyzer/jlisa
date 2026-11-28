package it.unive.jlisa.springed.security.p1.securitychains;

public class Rule {

	private final Integer ruleOrder;
	private final RuleMatcher ruleMatcher;
	private final Decision decision;

	public Rule(
			Integer ruleOrder,
			RuleMatcher ruleMatcher,
			Decision decision) {
		this.ruleOrder = ruleOrder;
		this.ruleMatcher = ruleMatcher;
		this.decision = decision;
	}

	public Integer getRuleOrder() {
		return ruleOrder;
	}

	public RuleMatcher getRuleMatcher() {
		return ruleMatcher;
	}

	public Decision getDecision() {
		return decision;
	}
}
