package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedStaticCall;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedSuperCall;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.UnitType;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

/**
 * Visitor that translates {@link MethodInvocation} and
 * {@link SuperMethodInvocation} JDT AST nodes into LiSA unresolved call
 * expressions.
 */
class MethodInvocationASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<Expression> {

	private Expression expression;

	MethodInvocationASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			MethodInvocation node) {
		List<Expression> parameters = new ArrayList<>();
		String methodName = node.getName().toString();
		ClassUnit classUnit = (ClassUnit) getScope().getCFG().getUnit();

		boolean isInstance = false;
		String name = null;
		if (node.getExpression() == null) {
			isInstance = !classUnit.getInstanceCodeMembersByName(methodName, true).isEmpty();
			if (isInstance)
				parameters.add(new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this",
						new JavaReferenceType(JavaClassType.lookup(classUnit.getName()))));
		} else {
			name = getScope().getParentScope().getUnitScope().getExplicitImports().get(node.getExpression().toString());
			if (name == null) {
				name = node.getExpression().toString();
				Unit resolved = TypeASTVisitor.getUnit(name, getProgram(), getScope().getParentScope().unitScope());
				if (resolved != null)
					name = resolved.getName();
			}
			if (LibrarySpecificationProvider.isLibraryAvailable(name))
				LibrarySpecificationProvider.importClass(getProgram(), name);

			Expression rec = null;
			try {
				rec = getParserContext().evaluate(node.getExpression(),
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
			} catch (ParsingException e) {
				if (!e.getName().equals("missing-variable"))
					throw e;
			}

			if (rec != null) {
				if (rec instanceof VariableRef) {
					if (rec.getStaticType() instanceof JavaReferenceType refType) {
						if (refType.getInnerType() instanceof UnitType unitType) {
							if (!unitType.getUnit().getInstanceCodeMembersByName(methodName, true).isEmpty()) {
								parameters.add(rec);
								isInstance = true;
							} else {
								name = unitType.toString();
							}
						}
					}
				} else if (rec instanceof JavaAccessGlobal accessGlobal) {
					if (accessGlobal.getTarget().getStaticType() instanceof JavaReferenceType refType) {
						if (refType.getInnerType() instanceof JavaClassType classType) {
							if (!classType.getUnit().getInstanceCodeMembersByName(methodName, true).isEmpty()) {
								parameters.add(rec);
								isInstance = true;
							} else {
								name = classType.toString();
							}
						}
					}
				} else {
					parameters.add(rec);
					isInstance = true;
				}
			}
		}

		if (!node.typeArguments().isEmpty())
			throw new ParsingException("method-invocation",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Method Invocation expressions with type arguments are not supported.",
					getSourceCodeLocation(node));

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				parameters.add(getParserContext().evaluate(e,
						() -> new ExpressionVisitor(getEnvironment(), getScope())));
			}
		}

		if (isInstance)
			expression = new JavaUnresolvedCall(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getName()).nextColumn(),
					Call.CallType.INSTANCE,
					null,
					node.getName().toString(),
					parameters.toArray(new Expression[0]));
		else
			expression = new JavaUnresolvedStaticCall(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getName()).nextColumn(),
					name == null ? classUnit.getName() : name,
					node.getName().toString(),
					parameters.toArray(new Expression[0]));

		return false;
	}

	@Override
	public boolean visit(
			SuperMethodInvocation node) {
		ClassUnit superClass = (ClassUnit) getScope().getCFG().getUnit();
		JavaClassType superType = JavaClassType.lookup(superClass.getName());

		List<Expression> parameters = new ArrayList<>();
		parameters.add(new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this",
				new JavaReferenceType(superType)));

		for (Object args : node.arguments()) {
			ASTNode e = (ASTNode) args;
			parameters.add(getParserContext().evaluate(e,
					() -> new ExpressionVisitor(getEnvironment(), getScope())));
		}

		expression = new JavaUnresolvedSuperCall(getScope().getCFG(),
				getSourceCodeLocationManager(node.getName()).nextColumn(),
				Call.CallType.INSTANCE, superClass.getName(), node.getName().toString(),
				parameters.toArray(new Expression[0]));
		return false;
	}

	@Override
	public Expression getResult() {
		return expression;
	}
}
