package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
		int modifiers = node.getModifiers();
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getUnitScope());
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String identifier = fragment.getName().getIdentifier();

			if (visitedFieldNames.contains(identifier))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Global variable " + identifier + " already exists in the cfg", getSourceCodeLocation(node));
			else
				visitedFieldNames.add(identifier);

			type = typeVisitor.liftToArray(type, fragment);
			boolean isStatic = Modifier.isStatic(modifiers) || (getScope().getLisaClassUnit() instanceof InterfaceUnit);
			Global global = new Global(getSourceCodeLocation(fragment), getScope().getLisaClassUnit(), identifier, !isStatic, type,
					new Annotations());
			if (isStatic) {
				getScope().getLisaClassUnit().addGlobal(global);
			} else {
				getScope().getLisaClassUnit().addInstanceGlobal(global);
			}
		}

		return false;
	}
}
