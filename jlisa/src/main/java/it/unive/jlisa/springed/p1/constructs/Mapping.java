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

	public WebAnnotation getAnnotation() { return annotation; }

	@JsonProperty("method")
	public String getMethodName() {
		return method.getDescriptor().getFullName();
	}

	@JsonIgnore
	public String getJsonFieldName() {
		String fullName = method.getDescriptor().getFullName();
		int separator = fullName.indexOf("::");

		String qualifiedClass = separator >= 0 ? fullName.substring(0, separator) : fullName;
		String methodName = separator >= 0 ? fullName.substring(separator + 2) : "";
		String className = qualifiedClass.substring(qualifiedClass.lastIndexOf('.') + 1);

		return className + "_" + methodName;
	}
}
