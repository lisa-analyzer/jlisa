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

    //  CLASS LEVEL (@RestController)

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
                AnnotationInfo info =
                        new AnnotationInfo(simple, Collections.emptyMap());
                parserContext.addClassAnnotation(clazzType, info);
            }
        }
    }

    //  FIELD LEVEL (@Autowired)

    public static void detectFieldAnnotations(
            ParserContext parserContext,
            JavaClassType clazzType,
            FieldDeclaration fieldDecl) {

        // All fragments of this declaration
        for (Object fragObj : fieldDecl.fragments()) {
            VariableDeclarationFragment frag =
                    (VariableDeclarationFragment) fragObj;

            String fieldName = frag.getName().getIdentifier();
            // key for map fields
            String fieldKey = clazzType + "::" + fieldName;

            for (Object m : fieldDecl.modifiers()) {
                if (!(m instanceof Annotation ann)) {
                    continue;
                }

                String simple = ann.getTypeName().getFullyQualifiedName();

                // just @Autowired
                if (matches(simple, AUTOWIRED)) {
                    AnnotationInfo info =
                            new AnnotationInfo(simple, Collections.emptyMap());
                    parserContext.addFieldAnnotation(fieldKey, info);
                }
            }
        }
    }

    // کمک‌کننده کوچک برای handle کردن نام کامل یا ساده
    private static boolean matches(String fullName, String simpleName) {
        if (fullName.equals(simpleName)) {
            return true;
        }
        return fullName.endsWith("." + simpleName);
    }
}
