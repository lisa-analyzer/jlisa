package it.unive.jlisa.frontend;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.VariableDeclarationASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.VoidType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jdt.core.dom.*;

public class InitCodeMembersASTVisitor extends ScopedVisitor<UnitScope> {

	public InitCodeMembersASTVisitor(
			ParsingEnvironment env,
			UnitScope scope) {
		super(env, scope);
	}

	public boolean visit(
			CompilationUnit node) {
		List<?> types = node.types();
		Set<String> processed = new TreeSet<>();
		for (Object type : types)
			if (type instanceof TypeDeclaration)
				initCodeMembersInDeclaration(node, (TypeDeclaration) type, null, processed);
			else if (type instanceof EnumDeclaration)
				initCodeMembersInEnum(node, (EnumDeclaration) type, null, processed);
		return false;
	}

	private void initCodeMembersInDeclaration(
			CompilationUnit unit,
			TypeDeclaration typeDecl,
			String outer,
			Set<String> processed) {
		String name = (getScope().getPackage().isEmpty() ? "" : getScope().getPackage() + ".")
				+ (outer == null ? "" : outer + ".") + typeDecl.getName().toString();
		if (!processed.add(name))
			return;

		it.unive.lisa.program.CompilationUnit lisaCU = null;
		if ((typeDecl.isInterface()))
			lisaCU = JavaInterfaceType.lookup(name).getUnit();
		else
			lisaCU = JavaClassType.lookup(name).getUnit();

		boolean isStatic = Modifier.isStatic(typeDecl.getModifiers());
		JavaClassType enclosing = outer == null || isStatic ? null
				: JavaClassType.lookup(getScope().getPackage() + outer);

		for (MethodDeclaration methodsDecl : typeDecl.getMethods()) {
			CodeMemberDescriptor codeMemberDescriptor;
			if (methodsDecl.isConstructor()) {
				codeMemberDescriptor = buildConstructorJavaCodeMemberDescriptor(methodsDecl, enclosing, lisaCU);
			} else {
				codeMemberDescriptor = buildJavaCodeMemberDescriptor(methodsDecl, lisaCU);
			}
			boolean isMain = isMain(methodsDecl);
			int modifiers = methodsDecl.getModifiers();
			CFG cfg = new CFG(codeMemberDescriptor);
			boolean added;
			if (!Modifier.isStatic(modifiers)) {
				added = lisaCU.addInstanceCodeMember(cfg);
			} else {
				added = lisaCU.addCodeMember(cfg);
			}
			if (!added)
				throw new ParsingException("duplicated_method_descriptor",
						ParsingException.Type.MALFORMED_SOURCE,
						"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + lisaCU.getName(),
						getSourceCodeLocation(methodsDecl));

			if (isMain)
				getProgram().addEntryPoint(cfg);
		}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? typeDecl.getName().toString() : outer + "." + typeDecl.getName().toString();
		TypeDeclaration[] nested = typeDecl.getTypes();
		for (TypeDeclaration n : nested)
			initCodeMembersInDeclaration(unit, n, newOuter, processed);
		for (Object decl : typeDecl.bodyDeclarations())
			if (decl instanceof TypeDeclaration)
				initCodeMembersInDeclaration(unit, (TypeDeclaration) decl, newOuter, processed);
			else if (decl instanceof EnumDeclaration)
				initCodeMembersInEnum(unit, (EnumDeclaration) decl, newOuter, processed);
	}

	private void initCodeMembersInEnum(
			CompilationUnit unit,
			EnumDeclaration node,
			String outer,
			Set<String> processed) {
		String name = getScope().getPackage() + (outer == null ? "" : outer + ".") + node.getName().toString();
		if (!processed.add(name))
			return;
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(name);

		for (Object decl : node.bodyDeclarations())
			if (decl instanceof MethodDeclaration) {
				MethodDeclaration methodsDecl = (MethodDeclaration) decl;
				CodeMemberDescriptor codeMemberDescriptor = buildJavaCodeMemberDescriptor(methodsDecl, enUnit);
				boolean isMain = isMain(methodsDecl);
				int modifiers = methodsDecl.getModifiers();
				CFG cfg = new CFG(codeMemberDescriptor);
				boolean added;
				if (!Modifier.isStatic(modifiers)) {
					added = enUnit.addInstanceCodeMember(cfg);
				} else {
					added = enUnit.addCodeMember(cfg);
				}
				if (!added)
					throw new ParsingException("duplicated_method_descriptor",
							ParsingException.Type.MALFORMED_SOURCE,
							"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + enUnit.getName(),
							getSourceCodeLocation(methodsDecl));

				if (isMain)
					getProgram().addEntryPoint(cfg);
			}

		// nested types (e.g., nested inner classes)
		String newOuter = outer == null ? node.getName().toString() : outer + "." + node.getName().toString();
		for (Object decl : node.bodyDeclarations())
			if (decl instanceof TypeDeclaration)
				initCodeMembersInDeclaration(unit, (TypeDeclaration) decl, newOuter, processed);
			else if (decl instanceof EnumDeclaration)
				initCodeMembersInEnum(unit, (EnumDeclaration) decl, newOuter, processed);
	}

	@SuppressWarnings("unchecked")
	private JavaCodeMemberDescriptor buildJavaCodeMemberDescriptor(
			MethodDeclaration node,
			it.unive.lisa.program.CompilationUnit lisaCU) {
		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());

		it.unive.lisa.type.Type returnType = null;

		// the method is generic
		if (node.typeParameters().stream().filter(tp -> tp.toString().equals(node.getReturnType2().toString()))
				.count() > 0)
			returnType = JavaClassType.getObjectType();
		// the method is not generic, but the class it is
		else {
			List<?> topLevelTypes = getAstUnit().types();
			for (Object tlType : topLevelTypes) {
				if (tlType instanceof TypeDeclaration)
					if (((TypeDeclaration) tlType).typeParameters().stream()
							.filter(tp -> tp.toString().equals(node.getReturnType2().toString())).count() > 0)
						returnType = JavaClassType.getObjectType();
			}

			if (returnType == null) {
				TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope());
				node.getReturnType2().accept(typeVisitor);
				returnType = typeVisitor.getType();
			}
		}

		List<Parameter> parameters = new ArrayList<>();
		if (instance) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisaCU.getName());
			parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
					new Annotations()));
		}

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(getEnvironment(), getScope());
			sd.accept(vd);
			parameters.add(vd.getResult());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisaCU, instance,
				node.getName().getIdentifier(),
				returnType.isInMemoryType() ? new JavaReferenceType(returnType) : returnType, annotations, paramArray);
		if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
			codeMemberDescriptor.setOverridable(false);
		} else {
			codeMemberDescriptor.setOverridable(true);
		}

		return codeMemberDescriptor;
	}

	private JavaCodeMemberDescriptor buildConstructorJavaCodeMemberDescriptor(
			MethodDeclaration node,
			JavaClassType enclosing,
			it.unive.lisa.program.CompilationUnit lisaCU) {

		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());
		it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisaCU.getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
				new Annotations()));

		if (enclosing != null)
			parameters.add(new Parameter(getSourceCodeLocationManager(node).nextColumn(), "$enclosing",
					enclosing.getReference(),
					null, new Annotations()));

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(getEnvironment(), getScope());
			sd.accept(vd);
			parameters.add(vd.getResult());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisaCU, instance,
				node.getName().getIdentifier(), VoidType.INSTANCE, annotations, paramArray);
		if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
			codeMemberDescriptor.setOverridable(false);
		} else {
			codeMemberDescriptor.setOverridable(true);
		}

		return codeMemberDescriptor;
	}

	private boolean isMain(
			MethodDeclaration node) {
		if (!Modifier.isStatic(node.getModifiers())) {
			return false;
		}
		if (!node.getName().getIdentifier().equals("main")) {
			return false;
		}
		if (node.getReceiverType() != null) {
			return false;
		}
		if (node.parameters().size() != 1) {
			return false;
		}
		SingleVariableDeclaration parameter = (SingleVariableDeclaration) node.parameters().getFirst();
		org.eclipse.jdt.core.dom.Type type = parameter.getType();
		if (parameter.getType().toString().equals("String[]")) {
			return true;
		}
		if (type instanceof SimpleType && ((SimpleType) type).getName().toString().equals("String")
				&& parameter.getExtraDimensions() == 1) {
			return true;
		}

		return false;
	}
}
