package it.unive.jlisa.frontend.visitors.expression;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Visitor that resolves {@link QualifiedName} and {@link SimpleName} JDT AST
 * nodes to LiSA expressions (variable references, field accesses, or static
 * global accesses).
 */
class NameResolverASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<Expression> {

	private Expression expression;

	NameResolverASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			QualifiedName node) {
		// based on tests, field accesses have precedence over fqns
		// the resolution also searches in enclosing instances
		try {
			Expression fa = solveAsFieldAccess(node);
			if (fa != null) {
				expression = fa;
				return false;
			}
		} catch (ParsingException e) {
			if (!e.getName().equals("missing-global"))
				throw e;
		}

		// we were not able to solve it as a field access,
		// so we might have:
		// - some.qualified.or.not.class.name.StaticField
		// - some.qualified.or.not.class.name.StaticField.SomeOtherField
		// - some.qualified.or.not.class.name (?)

		// we try to find a unit by starting at the first token
		// ("some") and going right ("some.qualified", "some.qualified.or", ...)
		// until we find a unit or we exhaust all the possibilities
		List<SimpleName> fields = new LinkedList<>();
		SimpleName firstField = null;
		Name current = node;
		// each element in names holds:
		// - the fqn to try
		// - the static field to access in the fqn
		// - the remaining fields to access from the static field
		LIFOWorkingSet<Triple<String, SimpleName, List<SimpleName>>> names = new LIFOWorkingSet<>();
		while (current != null) {
			names.push(Triple.of(current.toString(), firstField, new LinkedList<>(fields)));
			if (current instanceof QualifiedName) {
				if (firstField != null)
					fields.addFirst(firstField);
				firstField = ((QualifiedName) current).getName();
				current = ((QualifiedName) current).getQualifier();
			} else
				current = null;
		}

		Unit candidate = null;
		while (!names.isEmpty()) {
			Triple<String, SimpleName, List<SimpleName>> tentative = names.pop();
			candidate = TypeASTVisitor.getUnit(tentative.getLeft(), getProgram(),
					getScope().getParentScope().unitScope());
			if (candidate == null)
				continue;

			// unit found, search for the global
			if (tentative.getMiddle() == null)
				// if we got a unit with no field to access, we do nothing:
				// the caller has to handle the fqn as a type (eg, in a static
				// call)
				return false;
			Global global = getParserContext().getGlobal(candidate, tentative.getMiddle().getIdentifier(), false);
			if (global == null)
				continue;

			Expression access = new JavaAccessGlobal(
					getScope().getCFG(),
					getSourceCodeLocationManager(node.getQualifier(), true).getCurrentLocation(),
					candidate,
					global);

			if (tentative.getRight().isEmpty()) {
				expression = access;
				return false;
			}

			for (SimpleName f : tentative.getRight()) {
				try {
					access = new JavaAccessInstanceGlobal(getScope().getCFG(),
							getSourceCodeLocationManager(f).nextColumn(),
							access,
							f.getIdentifier());
				} catch (ParsingException e) {
					if (!e.getName().equals("missing-global"))
						throw e;
					break;
				}
			}
		}

		throw new ParsingException("missing-type",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Missing unit " + node,
				getSourceCodeLocation(node));
	}

	private Expression solveAsFieldAccess(
			QualifiedName node) {
		String targetName = node.getName().getIdentifier();
		Name qualifier = node.getQualifier();

		Expression receiver = null;
		if (qualifier instanceof SimpleName) {
			try {
				receiver = getParserContext().evaluate(
						(SimpleName) qualifier,
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
			} catch (ParsingException e) {
				if (!e.getName().equals("missing-variable"))
					throw e;
				receiver = null;
			}
		} else if (qualifier instanceof QualifiedName)
			receiver = solveAsFieldAccess((QualifiedName) qualifier);

		if (receiver == null)
			return null;

		if (receiver.getStaticType().isReferenceType()
				&& receiver.getStaticType().asReferenceType().getInnerType().isUnitType()) {
			UnitType unitType = receiver.getStaticType().asReferenceType().getInnerType().asUnitType();
			Global global = getParserContext().getGlobal(unitType.getUnit(), targetName, true);
			if (global != null && !global.isInstance())
				return new JavaAccessGlobal(
						getScope().getCFG(),
						getSourceCodeLocationManager(node.getQualifier(), true).getCurrentLocation(),
						unitType.getUnit(),
						global);
		}

		return new JavaAccessInstanceGlobal(getScope().getCFG(),
				getSourceCodeLocationManager(node.getQualifier(), true).nextColumn(),
				receiver,
				targetName);
	}

	@Override
	public boolean visit(
			SimpleName node) {
		String identifier = node.getIdentifier();
		if (getScope().getTracker() != null && getScope().getTracker().hasVariable(identifier)) {
			expression = new VariableRef(
					getScope().getCFG(),
					getSourceCodeLocation(node),
					identifier,
					getParserContext().getVariableStaticType(getScope().getCFG(), new VariableInfo(identifier,
							getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(identifier)
									: null)));
			return false;
		}

		Global global = getParserContext().getGlobal(getScope().getCFG().getDescriptor().getUnit(), identifier, true);
		SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());
		if (global != null) {
			if (global.isInstance()) {
				JavaReferenceType type = null;
				if (getScope().getCFG().getUnit() instanceof ClassUnit)
					type = JavaClassType.lookup(getScope().getCFG().getUnit().getName()).getReference();
				else
					type = JavaInterfaceType.lookup(getScope().getCFG().getUnit().getName()).getReference();

				expression = new JavaAccessInstanceGlobal(getScope().getCFG(),
						getSourceCodeLocationManager(node).getCurrentLocation(),
						new VariableRef(getScope().getCFG(), synth.nextLocation(), "this", type),
						identifier);
			} else
				expression = new JavaAccessGlobal(
						getScope().getCFG(),
						getSourceCodeLocationManager(node).getCurrentLocation(),
						getScope().getCFG().getUnit(),
						global);

			return false;
		}

		ClassScope cursor = getScope().getParentScope().getParentScope();
		JavaReferenceType type = null;
		if (getScope().getCFG().getUnit() instanceof ClassUnit)
			type = JavaClassType.lookup(getScope().getCFG().getUnit().getName()).getReference();
		else
			type = JavaInterfaceType.lookup(getScope().getCFG().getUnit().getName()).getReference();
		expression = new VariableRef(getScope().getCFG(), synth.nextLocation(), "this", type);
		while (cursor != null) {
			CompilationUnit encl = cursor.getLisaClassUnit();
			expression = new JavaAccessInstanceGlobal(getScope().getCFG(), synth.nextLocation(), expression,
					"$enclosing");
			global = getParserContext().getGlobal(encl, identifier, true);
			if (global != null) {
				if (global.isInstance()) {
					expression = new JavaAccessInstanceGlobal(
							getScope().getCFG(),
							getSourceCodeLocation(node),
							expression,
							identifier);
				} else
					expression = new JavaAccessGlobal(
							getScope().getCFG(),
							getSourceCodeLocation(node),
							encl,
							global);

				return false;
			} else
				cursor = cursor.getParentScope();
		}

		throw new ParsingException("missing-variable",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Variable " + identifier + " not defined before its use",
				getSourceCodeLocation(node));
	}

	@Override
	public Expression getResult() {
		return expression;
	}
}
