package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedStaticCall;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.*;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ClassASTVisitor extends JavaASTVisitor {

	private boolean nested;

	public ClassASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit) {
		super(parserContext, source, compilationUnit);
		nested = false;
	}

	public ClassASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			boolean nested) {
		super(parserContext, source, compilationUnit);
		this.nested = nested;
	}

	@Override
	public boolean visit(
			EnumDeclaration node) {
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(node.getName().toString());
		Type enumType = getProgram().getTypes().getType(enUnit.getName());

		// adding static fields corresponding to enum constants
		for (Object con : node.enumConstants()) {
			Global g = new Global(getSourceCodeLocation((ASTNode) con), enUnit, con.toString(), false,
					new JavaReferenceType(enumType));
			enUnit.addGlobal(g);
		}

		// build the enum constructor (for initializing fields)
		createEnumConstructor(enUnit);

		// build enum static initializer
		createEnumInitializer(enUnit);
		return false;
	}

	private void computeNestedUnits(
			TypeDeclaration typeDecl) {
		if ((typeDecl.isInterface())) {
			InterfaceUnit iUnit = buildInterfaceUnit(source, getProgram(), typeDecl);
			getProgram().getTypes().registerType(JavaInterfaceType.lookup(iUnit.getName(), iUnit));
			getProgram().addUnit(iUnit);
		} else {
			ClassUnit cUnit = buildClassUnit(source, getProgram(), typeDecl);
			getProgram().getTypes().registerType(JavaClassType.lookup(cUnit.getName(), cUnit));
			getProgram().addUnit(cUnit);
		}
	}

	private InterfaceUnit buildInterfaceUnit(
			String source,
			Program program,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		if (Modifier.isFinal(modifiers)) {
			throw new RuntimeException(
					new ProgramValidationException("Illegal combination of modifiers: interface and final"));
		}

		InterfaceUnit iUnit = new InterfaceUnit(loc, program, typeDecl.getName().toString(), false);
		program.addUnit(iUnit);
		return iUnit;
	}

	private ClassUnit buildClassUnit(
			String source,
			Program program,
			TypeDeclaration typeDecl) {
		SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

		int modifiers = typeDecl.getModifiers();
		// FIXME: nested inner class can be private, but we currently do not
		// model it (treated as public)
		ClassUnit cUnit;
		if (Modifier.isAbstract(modifiers)) {
			if (Modifier.isFinal(modifiers)) {
				throw new RuntimeException(
						new ProgramValidationException("illegal combination of modifiers: abstract and final"));
			}
			cUnit = new AbstractClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
		} else {
			cUnit = new ClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
		}
		program.addUnit(cUnit);
		return cUnit;
	}

	@Override
	public boolean visit(
			TypeDeclaration node) {
		TypeDeclaration[] types = node.getTypes(); // nested types (e.g., nested
													// inner classes)

		for (TypeDeclaration type : types) {
			if (type instanceof TypeDeclaration) {
				TypeDeclaration typeDecl = (TypeDeclaration) type;
				if ((typeDecl.isInterface())) {
					InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(parserContext, source,
							compilationUnit);
					typeDecl.accept(interfaceVisitor);
				} else {
					ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, compilationUnit, true);
					typeDecl.accept(classVisitor);
				}
			}
		}

		if (nested)
			computeNestedUnits(node);

		// parsing superclass
		ClassUnit cUnit = (ClassUnit) getProgram().getUnit(node.getName().toString());
		if (node.getSuperclassType() != null) {
			TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
			node.getSuperclassType().accept(visitor);
			Type superType = visitor.getType();
			if (superType != null) {
				it.unive.lisa.program.Unit superUnit = getProgram().getUnit(superType.toString());
				if (superUnit instanceof it.unive.lisa.program.CompilationUnit) {
					cUnit.addAncestor((it.unive.lisa.program.CompilationUnit) superUnit);
				}
			}
		} else {
			cUnit.addAncestor(JavaClassType.getObjectType().getUnit());
		}

		// parsing implemented interfaces
		for (Object intf : node.superInterfaceTypes()) {
			TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
			((ASTNode) intf).accept(visitor);
			Type intfType = visitor.getType();
			it.unive.lisa.program.Unit intfUnit = getProgram().getUnit(intfType.toString());
			if (intfUnit instanceof it.unive.lisa.program.CompilationUnit) {
				cUnit.addAncestor((it.unive.lisa.program.CompilationUnit) intfUnit);
			}
		}

		if (!node.permittedTypes().isEmpty()) {
			parserContext.addException(new ParsingException("permits", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Permits is not supported.", getSourceCodeLocation(node)));
		}

		// iterates over inner declarations (just enums)
		for (Object decl : node.bodyDeclarations()) {
			// enum inner declaration
			if (decl instanceof EnumDeclaration)
				visit((EnumDeclaration) decl);
		}

		// all fields (static and non-static) are visited
		Set<String> visitedFieldNames = new HashSet<>();
		for (FieldDeclaration fd : node.getFields()) {
			FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit,
					visitedFieldNames);
			fd.accept(visitor);
		}

		createClassInitializer(cUnit, node);

		boolean createDefaultConstructor = true;
		for (MethodDeclaration md : node.getMethods()) {
			MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
			md.accept(visitor);
			if (md.isConstructor()) {
				createDefaultConstructor = false;
				fixConstructorCFG(visitor.getCFG(), node.getFields());
			}
		}
		if (createDefaultConstructor) {
			CFG defaultConstructor = createDefaultConstructor(cUnit);
			fixConstructorCFG(defaultConstructor, node.getFields());
		}

		return false;
	}

	private void createEnumInitializer(
			EnumUnit enumUnit) {

		SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
		CodeMemberDescriptor cmDesc = new CodeMemberDescriptor(locationManager.nextLocation(), enumUnit, false,
				enumUnit.getName() + InitializedClassSet.SUFFIX_CLINIT, VoidType.INSTANCE, new Annotations(),
				new Parameter[0]);
		CFG cfg = new CFG(cmDesc);

		// in the main method, we instantiate enum constants
		it.unive.lisa.type.Type enumType = getProgram().getTypes().getType(enumUnit.getName());

		Statement init = null, last = null;
		for (Global target : enumUnit.getGlobals()) {
			JavaAccessGlobal accessGlobal = new JavaAccessGlobal(cfg, locationManager.nextLocation(), enumUnit, target);
			JavaNewObj call = new JavaNewObj(cfg, locationManager.nextLocation(), enumUnit.getName(),
					new JavaReferenceType(enumType),
					new JavaStringLiteral(cfg, locationManager.nextLocation(), target.getName()));
			JavaAssignment asg = new JavaAssignment(cfg, locationManager.nextLocation(), accessGlobal, call);
			cfg.addNode(asg);

			if (init == null)
				init = asg;
			else
				cfg.addEdge(new SequentialEdge(last, asg));

			last = asg;
		}

		Ret ret = new Ret(cfg, locationManager.nextLocation());
		cfg.addNode(ret);
		cfg.addEdge(new SequentialEdge(last, ret));
		enumUnit.addCodeMember(cfg);
		cfg.getEntrypoints().add(init);
		return;
	}

	private void createEnumConstructor(
			EnumUnit enumUnit) {
		Type type = getProgram().getTypes().getType(enumUnit.getName());
		SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(locationManager.nextLocation(), "this", new JavaReferenceType(type), null,
				new Annotations()));
		parameters.add(new Parameter(locationManager.nextLocation(), "name",
				new JavaReferenceType(getProgram().getTypes().getStringType()), null, new Annotations()));

		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(locationManager.nextLocation(), enumUnit,
				true, enumUnit.getName(), VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, new VariableInfo("this", null), new JavaReferenceType(type));
		parserContext.addVariableType(cfg, new VariableInfo("name", null),
				new JavaReferenceType(getProgram().getTypes().getStringType()));

		JavaAssignment glAsg = new JavaAssignment(cfg, locationManager.nextLocation(),
				new JavaAccessInstanceGlobal(cfg, locationManager.nextLocation(),
						new VariableRef(cfg, locationManager.nextLocation(), "this"),
						"name"),
				new VariableRef(cfg, locationManager.nextLocation(), "name"));

		Ret ret = new Ret(cfg, locationManager.nextLocation());
		cfg.addNode(glAsg);
		cfg.addNode(ret);
		cfg.getEntrypoints().add(glAsg);
		cfg.addEdge(new SequentialEdge(glAsg, ret));
		enumUnit.addInstanceCodeMember(cfg);
	}

	private void createClassInitializer(
			ClassUnit unit,
			TypeDeclaration node) {

		// we add a class initializer only if the class has
		// static fields
		Set<FieldDeclaration> staticFields = new LinkedHashSet<FieldDeclaration>();
		for (FieldDeclaration fd : node.getFields()) {
			if (Modifier.isStatic(fd.getModifiers()))
				staticFields.add(fd);
		}

		if (staticFields.isEmpty())
			return;

		// create the CFG corresponding to the class initializer
		SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
		CodeMemberDescriptor cmDesc = new CodeMemberDescriptor(locationManager.nextLocation(), unit, false,
				unit.getName() + InitializedClassSet.SUFFIX_CLINIT, VoidType.INSTANCE, new Annotations(),
				new Parameter[0]);
		CFG cfg = new CFG(cmDesc);

		// first, we add the clinit call to the superclass
		Set<it.unive.lisa.program.CompilationUnit> superClasses = unit
				.getImmediateAncestors().stream()
				.filter(u -> u instanceof ClassUnit)
				.collect(Collectors.toSet());

		// we can safely suppose that there exist a single superclass
		ClassUnit superClass = (ClassUnit) superClasses.stream().findFirst().get();
		JavaUnresolvedStaticCall superClassInit = new JavaUnresolvedStaticCall(
				cfg,
				locationManager.nextLocation(),
				superClass.toString(),
				superClass.toString() + InitializedClassSet.SUFFIX_CLINIT,
				new Expression[0]);

		cfg.addNode(superClassInit);
		cfg.getEntrypoints().add(superClassInit);

		Statement last = superClassInit;

		// just static fields are considered to build the class initializer
		for (FieldDeclaration fd : staticFields) {
			TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
			fd.getType().accept(typeVisitor);
			Type type = typeVisitor.getType();
			if (type.isInMemoryType())
				type = new JavaReferenceType(type);

			for (Object f : fd.fragments()) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
				it.unive.lisa.program.cfg.statement.Expression init;
				if (fragment.getInitializer() != null) {
					ExpressionVisitor exprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
							null);
					fragment.getInitializer().accept(exprVisitor);
					init = exprVisitor.getExpression();
				} else
					init = type.defaultValue(cfg, locationManager.nextLocation());

				Global global = new Global(
						locationManager.nextLocation(),
						unit,
						fragment.getName().getIdentifier(),
						false,
						Untyped.INSTANCE, // fixme: check how to get type for
											// arrays (use extraDimensions).
						new Annotations());
				JavaAccessGlobal accessGlobal = new JavaAccessGlobal(cfg, locationManager.nextLocation(), unit, global);
				JavaAssignment asg = new JavaAssignment(cfg, locationManager.nextLocation(), accessGlobal, init);
				cfg.addNode(asg);
				cfg.addEdge(new SequentialEdge(last, asg));
				last = asg;
			}
		}

		// TODO: static block
		Ret ret = new Ret(cfg, locationManager.nextLocation());
		cfg.addNode(ret);
		cfg.addEdge(new SequentialEdge(last, ret));
		unit.addCodeMember(cfg);
		return;
	}

	private CFG createDefaultConstructor(
			ClassUnit classUnit) {
		Type type = getProgram().getTypes().getType(classUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
		parameters.add(new Parameter(locationManager.nextLocation(), "this", new JavaReferenceType(type), null,
				new Annotations()));

		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(locationManager.nextLocation(), classUnit,
				true, classUnit.getName(), VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, new VariableInfo("this", null), new JavaReferenceType(type));
		// we filter just the class unit, not interfaces
		String superClassName = classUnit.getImmediateAncestors().stream().filter(s -> s instanceof ClassUnit)
				.findFirst().get().getName();

		JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, locationManager.nextLocation(), Call.CallType.INSTANCE,
				null, superClassName, new VariableRef(cfg, locationManager.nextLocation(), "this"));

		Ret ret = new Ret(cfg, locationManager.nextLocation());
		cfg.addNode(ret);
		cfg.addNode(call);
		cfg.getEntrypoints().add(call);
		cfg.addEdge(new SequentialEdge(call, ret));
		classUnit.addInstanceCodeMember(cfg);
		return cfg;
	}

	private void fixConstructorCFG(
			CFG cfg,
			FieldDeclaration[] fields) {
		Statement entryPoint = cfg.getEntrypoints().iterator().next();
		Statement injectionPoint = entryPoint;
		Unit u = cfg.getDescriptor().getUnit();
		if (!(u instanceof ClassUnit)) {
			throw new RuntimeException("The unit of a constructor must be a class unit");
		}
		ClassUnit classUnit = (ClassUnit) u;
		// we filter just the class unit, not interfaces
		Unit ancestor = classUnit.getImmediateAncestors().stream().filter(s -> s instanceof ClassUnit).findFirst()
				.get();
		boolean implicitlyCallSuper = true;
		if (injectionPoint instanceof JavaUnresolvedCall call) {
			if (ancestor.getName().equals(call.getConstructName())) {
				implicitlyCallSuper = false;
				List<Edge> outEdges = new ArrayList<>(cfg.getNodeList().getOutgoingEdges(injectionPoint));
				if (outEdges.size() == 1) {
					injectionPoint = outEdges.getFirst().getDestination();
				}
			}
		}

		if (injectionPoint instanceof JavaUnresolvedCall &&
				((JavaUnresolvedCall) injectionPoint).getConstructName().equals(cfg.getDescriptor().getName())) {
			return;
		}
		if (implicitlyCallSuper) {
			// add a super() call to this constructor, as a first statement,
			// before the field initializator.
			SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
			JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, locationManager.nextLocation(),
					Call.CallType.INSTANCE, null, ancestor.getName(),
					new VariableRef(cfg, locationManager.nextLocation(), "this"));
			cfg.addNode(call);
			cfg.addEdge(new SequentialEdge(call, injectionPoint));
			cfg.getEntrypoints().clear();
			cfg.getEntrypoints().add(call);
			entryPoint = call;
		}
		Statement first = null, last = null;

		for (FieldDeclaration field : fields) {
			// static fields are skipped in constructor
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			FieldInitializationVisitor initVisitor = new FieldInitializationVisitor(parserContext, source,
					compilationUnit, cfg);
			field.accept(initVisitor);

			if (initVisitor.getBlock() != null) {
				cfg.getNodeList().mergeWith(initVisitor.getBlock());

				if (first == null) {
					first = initVisitor.getFirst();
				} else {
					cfg.addEdge(new SequentialEdge(last, initVisitor.getFirst()));
				}
				last = initVisitor.getLast();
			}
		}

		if (first != null) {
			if (injectionPoint.equals(entryPoint)) {
				cfg.getEntrypoints().clear();
				cfg.getEntrypoints().add(first);
				cfg.addEdge(new SequentialEdge(last, entryPoint));
			} else {
				for (Edge edge : cfg.getIngoingEdges(injectionPoint)) {
					cfg.getNodeList().removeEdge(edge);
					cfg.addEdge(new SequentialEdge(edge.getSource(), first));
				}
				cfg.addEdge(new SequentialEdge(last, injectionPoint));
			}
		}
	}
}