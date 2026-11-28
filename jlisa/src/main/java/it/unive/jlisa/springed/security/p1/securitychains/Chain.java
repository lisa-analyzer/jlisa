package it.unive.jlisa.springed.security.p1.securitychains;

import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class Chain {

	private final Integer chainOrder;
	private final MethodDeclaration method;
	private final List<String> chainMatchers;
	private final List<Rule> rules;

	public Chain(
			Integer chainOrder,
			MethodDeclaration method,
			List<String> chainMatchers,
			List<Rule> rules) {
		this.chainOrder = chainOrder;
		this.method = method;
		this.chainMatchers = chainMatchers;
		this.rules = rules;
	}

	public int getChainOrder() {
		return this.chainOrder;
	}

	public MethodDeclaration getMethod() {
		return this.method;
	}

	public List<Rule> getRules() {
		return this.rules;
	}

	public List<String> getChainMatchers() {
		return this.chainMatchers;
	}
}
