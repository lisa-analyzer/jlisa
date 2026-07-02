package it.unive.jlisa.frontend.visitors.structure;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.AnnotationBuilder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldDeclarationVisitor extends ScopedVisitor<ClassScope> {

	Set<String> visitedFieldNames;

	public FieldDeclarationVisitor(
			ParsingEnvironment environment,
			ClassScope scope,
			Set<String> visitedFieldNames) {
		super(environment, scope);
		this.visitedFieldNames = visitedFieldNames;
	}

	@Override
	public boolean visit(
			FieldDeclaration node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getUnitScope());
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);

		Annotations annotations = AnnotationBuilder.fromDeclarationModifiers(node.modifiers(),
				getEnvironment(), getScope().getUnitScope());

		CompilationUnit unit = getScope().getLisaClassUnit();

		int modifiers = node.getModifiers();
		boolean isStatic = Modifier.isStatic(modifiers) || (unit instanceof InterfaceUnit);
		boolean isFinal = Modifier.isFinal(modifiers);

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String identifier = fragment.getName().getIdentifier();

			if (!visitedFieldNames.add(identifier))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Global variable " + identifier + " already exists in the cfg", getSourceCodeLocation(node));

			type = typeVisitor.liftToArray(type, fragment);
			var loc = getSourceCodeLocation(fragment);

			Global global = isStatic && isFinal
					? new ConstantGlobal(loc, unit, identifier, new Constant(type, fragment, loc), annotations)
					: new Global(loc, unit, identifier, !isStatic, type, annotations);

			if (isStatic)
				unit.addGlobal(global);
			else
				unit.addInstanceGlobal(global);
		}

		return false;
	}
}
