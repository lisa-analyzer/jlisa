package it.unive.jlisa.springed.p1.constructs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.program.cfg.CodeMember;

public class Mapping {

	private final CodeMember method;
	private final WebAnnotation annotation;

	public Mapping(
			CodeMember method,
			WebAnnotation annotation) {
		this.method = method;
		this.annotation = annotation;
	}

	@JsonIgnore
	public CodeMember getMethod() {
		return method;
	}

	@JsonProperty("method")
	public String getMethodName() {
		return method.getDescriptor().getFullName();
	}

	public WebAnnotation getAnnotation() {
		return annotation;
	}
}
