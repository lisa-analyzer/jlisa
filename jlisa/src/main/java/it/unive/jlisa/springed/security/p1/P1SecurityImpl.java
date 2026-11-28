package it.unive.jlisa.springed.security.p1;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.springed.security.p1.securitychains.SecurityChains;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class P1SecurityImpl implements P1Security {

	private final ParserContext parserContext;

	public P1SecurityImpl(
			ParserContext parserContext) {
		this.parserContext = parserContext;
	}

	@Override
	public SecurityChains p1(
			Unit[] p) {
		SecurityChains securityChains = new SecurityChains();

		List<CodeMember> securityFilterChains = this.getSecurityFilterChains(p);
		return null;
	}

	public List<CodeMember> getSecurityFilterChains(
			Unit[] units) {
		Collection<CodeMember> securityFilterChains = new ArrayList<>();

		List<ClassUnit> classUnits = new ArrayList<>();
		for (Unit unit : units) {
			if (unit instanceof ClassUnit) {
				classUnits.add((ClassUnit) unit);
			}
		}

		for (ClassUnit classUnit : classUnits) {
			for (CodeMember codeMember : classUnit.getInstanceCodeMembers(false)) {
				if (isAnnotatedAsBean(codeMember) && isOfTypeSecurityFilterChain(codeMember)) {
					securityFilterChains.add(codeMember);
				}
			}
		}

		return securityFilterChains.stream().toList();
	}

	private boolean isAnnotatedAsBean(
			CodeMember method) {
		Collection<Annotation> methodAnnotations = method.getDescriptor().getAnnotationList();

		for (Annotation annotation : methodAnnotations) {
			if (annotation.getAnnotationName().equals("Bean")) {
				return true;
			}
		}

		return false;
	}

	private boolean isOfTypeSecurityFilterChain(
			CodeMember method) {
		Type type = method.getDescriptor().getReturnType();
		return type.toString().equals("org.springframework.security.web.SecurityFilterChain");
	}
}
