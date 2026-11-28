package it.unive.jlisa.springed.security.p1.securitychains;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class SecurityChains {

	private final List<Chain> chains = new ArrayList<>();

	public List<Chain> getChains() {
		return this.chains;
	}

	public void insert(
			Chain chain) {
		this.chains.add(chain);
	}

	public void insert(
			int chainOrder,
			MethodDeclaration method,
			List<String> chainMatchers,
			List<Rule> rules) {
		this.chains.add(new Chain(chainOrder, method, chainMatchers, rules));
	}
}
