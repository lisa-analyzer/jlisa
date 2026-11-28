package it.unive.jlisa.springed.security.p1.securitychains;

import java.util.List;

public class RuleMatcher {

	private final RuleType type;
	private final HttpMethod httpMethod;
	private final List<String> patterns;
	private final String obstruct;

	public RuleMatcher(
			RuleType type,
			HttpMethod httpMethod,
			List<String> patterns,
			String obstruct) {
		this.type = type;
		this.httpMethod = httpMethod;
		this.patterns = patterns;
		this.obstruct = obstruct;
	}

	public String getObstruct() {
		return obstruct;
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public RuleType getType() {
		return type;
	}
}
