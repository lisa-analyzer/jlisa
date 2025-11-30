package it.unive.jlisa.frontend.annotations;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

public final class MethodAnnotationExtractor {

	// All Spring mapping annotations we currently support
	private static final Set<
			String> SUPPORTED_MAPPINGS = Set.of("GetMapping", "PostMapping", "PutMapping", "DeleteMapping");

	// ... existing fields / methods

	/**
	 * Detects @GetMapping on a method, registers it in the ParserContext and
	 * returns the created AnnotationInfo (or null if not present).
	 */
	public static AnnotationInfo detectAndRegisterGetMapping(
			ParserContext parserContext,
			CodeMemberDescriptor member,
			MethodDeclaration node) {

		AnnotationInfo gmInfo = null;

		// loop on all annotations of the method
		for (Object m : (List<?>) node.modifiers()) {
			if (!(m instanceof Annotation ann))
				continue;

			// simple name without package prefix
			String simple = ann.getTypeName().getFullyQualifiedName();
			int dot = simple.lastIndexOf('.');
			if (dot >= 0)
				simple = simple.substring(dot + 1);

			// only care about mapping annotations we support
			if (!SUPPORTED_MAPPINGS.contains(simple)) {
				// if it's not one of
				// GetMapping/PostMapping/PutMapping/DeleteMapping, skip it
				continue;
			}

			Map<String, String> params = new HashMap<>();

			// @GetMapping("/user/info")
			if (ann instanceof SingleMemberAnnotation sma) {
				params.put("value", stripQuotes(String.valueOf(sma.getValue())));
			}
			// @GetMapping(value = "/user/info", path = "/user/info")
			else if (ann instanceof NormalAnnotation na) {
				for (Object o : na.values()) {
					MemberValuePair p = (MemberValuePair) o;
					params.put(
							p.getName().getIdentifier(),
							stripQuotes(String.valueOf(p.getValue())));
				}
			}

			// if only "path" is present, copy it into "value"
			if (!params.containsKey("value") && params.containsKey("path")) {
				params.put("value", params.get("path"));
			}

			// use the actual annotation simple name
			// (GetMapping/PostMapping/...)
			gmInfo = new AnnotationInfo(
					simple,
					Collections.unmodifiableMap(params));

			// register annotation info in the parser context
			parserContext.addMethodAnnotation(member, gmInfo);
		}
// debug: print extracted annotation, if any
		if (gmInfo != null) {
			System.out.println("[ANN] Found GetMapping on "
					+ member.getFullSignature()
					+ " -> " + gmInfo);
		}

		return gmInfo;
	}

	// removes surrounding quotes from a string literal
	private static String stripQuotes(
			String s) {
		if (s == null || s.length() < 2)
			return s;

		if ((s.startsWith("\"") && s.endsWith("\""))
				|| (s.startsWith("'") && s.endsWith("'"))) {
			return s.substring(1, s.length() - 1);
		}

		return s;
	}
}
