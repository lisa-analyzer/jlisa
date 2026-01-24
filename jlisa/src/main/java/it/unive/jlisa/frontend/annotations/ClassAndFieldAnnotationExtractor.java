package it.unive.jlisa.frontend.annotations;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.program.type.JavaClassType;
import java.util.Collections;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public final class ClassAndFieldAnnotationExtractor {

	private static final String REST_CONTROLLER = "RestController";
	private static final String AUTOWIRED = "Autowired";

	private ClassAndFieldAnnotationExtractor() {
		// utility class
	}

	// CLASS LEVEL (@RestController)

	public static void detectClassAnnotations(
			ParserContext parserContext,
			JavaClassType clazzType,
			TypeDeclaration node) {

		for (Object m : node.modifiers()) {
			if (!(m instanceof Annotation ann)) {
				continue;
			}

			String simple = ann.getTypeName().getFullyQualifiedName();

			// just @RestController
			if (matches(simple, REST_CONTROLLER)) {
				AnnotationInfo info = new AnnotationInfo(simple, Collections.emptyMap());
				parserContext.addClassAnnotation(clazzType, info);
			}
		}
	}

	// FIELD LEVEL (@Autowired)

	public static void detectFieldAnnotations(
			ParserContext parserContext,
			JavaClassType clazzType,
			FieldDeclaration fieldDecl) {

		// if THIS field declaration has @Autowired
		boolean hasAutowired = false;
		for (Object m : fieldDecl.modifiers()) {
			if (!(m instanceof Annotation ann))
				continue;

			String fullName = ann.getTypeName().getFullyQualifiedName();
			if (matches(fullName, AUTOWIRED)) {
				hasAutowired = true;
				break;
			}
		}

		// If no @Autowired on this declaration, stop here
		if (!hasAutowired)
			return;

		// 2) If it has @Autowired, apply it to all fragments declared in the same line
		for (Object fragObj : fieldDecl.fragments()) {
			if (!(fragObj instanceof VariableDeclarationFragment frag))
				continue;

			String fieldName = frag.getName().getIdentifier();

			String fieldKey = ParserContext.fieldKey(clazzType.toString(), fieldName);


			//  store the SIMPLE name, not the fully-qualified name
			AnnotationInfo info = new AnnotationInfo(AUTOWIRED, Collections.emptyMap());
			parserContext.addFieldAnnotation(fieldKey, info);
		}
	}


	// help for handle
	private static boolean matches(
			String fullName,
			String simpleName) {
		if (fullName.equals(simpleName)) {
			return true;
		}
		return fullName.endsWith("." + simpleName);
	}
}
