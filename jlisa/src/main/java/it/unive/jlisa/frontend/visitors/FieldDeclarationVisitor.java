package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class FieldDeclarationVisitor extends JavaASTVisitor {
	it.unive.lisa.program.CompilationUnit lisacompilationUnit;

	public FieldDeclarationVisitor(ParserContext parserContext, String source, it.unive.lisa.program.CompilationUnit lisacompilationUnit, CompilationUnit astCompilationUnit) {
		super(parserContext, source, astCompilationUnit);
		this.lisacompilationUnit = lisacompilationUnit;
	}
	@Override
	public boolean visit(FieldDeclaration node) {
		int modifiers = node.getModifiers();
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new ReferenceType(type);

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
			boolean isStatic = Modifier.isStatic(modifiers);
			Global global = new Global(getSourceCodeLocation(fragment), lisacompilationUnit, identifier, !isStatic, type, new Annotations());
			if (isStatic) {
				lisacompilationUnit.addGlobal(global);
			} else {
				lisacompilationUnit.addInstanceGlobal(global);
			}
		}

		return false;
	}
}
