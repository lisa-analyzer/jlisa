package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class VariableDeclarationASTVisitor extends ScopedVisitor<UnitScope> implements ResultHolder<Parameter> {
	Parameter parameter;

	public VariableDeclarationASTVisitor(
			ParsingEnvironment environment,
			UnitScope scope) {
		super(environment, scope);
	}

	public boolean visit(
			SingleVariableDeclaration node) {
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope());
		node.getType().accept(visitor);
		Type type = visitor.getType();
		type = type.isInMemoryType() ? new JavaReferenceType(type) : type;
		if (node.getExtraDimensions() != 0) {
			if (type instanceof ArrayType) {
				ArrayType arrayType = (ArrayType) type;
				int dim = arrayType.getDimensions();
				type = JavaArrayType.lookup(arrayType.getBaseType(), dim + node.getExtraDimensions());
			} else {
				type = JavaArrayType.lookup(type, node.getExtraDimensions());
			}
		}

		type = type.isInMemoryType() ? new JavaReferenceType(type) : type;

		String identifier = node.getName().getIdentifier();
		// TODO annotations
		Annotations annotations = new Annotations();
		this.parameter = new Parameter(getSourceCodeLocation(node), identifier, type, null, annotations);
		return false;
	}

	@Override
	public Parameter getResult() {
		return parameter;
	}
}
