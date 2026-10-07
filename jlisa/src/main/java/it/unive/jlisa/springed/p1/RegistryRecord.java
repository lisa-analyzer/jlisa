package it.unive.jlisa.springed.p1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import java.util.StringJoiner;

public class RegistryRecord {

	private final CodeMember method;
	private final RequestMapping annotation;

	public RegistryRecord(
			CodeMember method,
			RequestMapping annotation) {
		this.method = method;
		this.annotation = annotation;
	}

	@JsonIgnore
	public CodeMember getMethod() {
		return method;
	}

	public RequestMapping getAnnotation() {
		return annotation;
	}

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

	@JsonIgnore
	public String getJsonFieldNameWithParameters() {
		CodeMemberDescriptor descriptor = method.getDescriptor();
		Parameter[] formals = descriptor.getFormals();
		StringJoiner types = new StringJoiner(", ", "(", ")");

		for (int i = descriptor.isInstance() ? 1 : 0; i < formals.length; i++) {
			String type = formals[i].getStaticType().toString().replace("*", "");
			types.add(type.substring(type.lastIndexOf('.') + 1));
		}

		return getJsonFieldName() + types;
	}
}
