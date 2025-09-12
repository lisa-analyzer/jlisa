package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldDeclarationVisitor extends JavaASTVisitor {
	it.unive.lisa.program.CompilationUnit unit;

	Set<String> visitedFieldNames;

	public FieldDeclarationVisitor(
			ParserContext parserContext,
			String source,
			it.unive.lisa.program.CompilationUnit lisacompilationUnit,
			CompilationUnit astCompilationUnit,
			Set<String> visitedFieldNames) {
		super(parserContext, source, astCompilationUnit);
		this.unit = lisacompilationUnit;
		this.visitedFieldNames = visitedFieldNames;
	}

	@Override
	public boolean visit(
			FieldDeclaration node) {
		int modifiers = node.getModifiers();
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			if (fragment.getExtraDimensions() != 0) {
				if (type instanceof ArrayType) {
					ArrayType arrayType = (ArrayType) type;
					int dim = arrayType.getDimensions();
					type = JavaArrayType.lookup(arrayType.getBaseType(), dim + fragment.getExtraDimensions());
				} else {
					type = JavaArrayType.lookup(type, fragment.getExtraDimensions());
				}
			}

			String identifier = fragment.getName().getIdentifier();

			if (visitedFieldNames.contains(identifier))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Global variable " + identifier + " already exists in the cfg", getSourceCodeLocation(node));
			else
				visitedFieldNames.add(identifier);

			boolean isStatic = Modifier.isStatic(modifiers);
			Global global = new Global(getSourceCodeLocation(fragment), unit, identifier, !isStatic, type,
					new Annotations());
			if (isStatic) {
				unit.addGlobal(global);
			} else {
				unit.addInstanceGlobal(global);
			}
		}

		return false;
	}
}
