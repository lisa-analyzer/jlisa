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
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
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
import it.unive.lisa.type.VoidType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ClassASTVisitor extends BaseUnitASTVisitor {

	private final String fullName;
	public final ClassASTVisitor enclosing;
	public final JavaClassType enclosingType;

	public ClassASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			String pkg,
			Map<String, String> imports,
			String fullName,
			ClassASTVisitor enclosing,
			JavaClassType enclosingType) {
		super(parserContext, source, pkg, imports, compilationUnit);
		this.fullName = fullName;
		this.enclosing = enclosing;
		this.enclosingType = enclosingType;
	}

	public boolean visit(
			AnonymousClassDeclaration node) {
		// parsing superclass
		ClassUnit cUnit = (ClassUnit) getProgram().getUnit(fullName);
		
		boolean createDefaultConstructor = true;
		for (Object md : node.bodyDeclarations()) {
			MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit, false, this,
					enclosingType);
			((ASTNode) md).accept(visitor);
			if (((MethodDeclaration) md).isConstructor()) {
				createDefaultConstructor = false;
				fixConstructorCFG(visitor.getCFG(), new FieldDeclaration[0]);
			}
		}
		if (createDefaultConstructor) {
			CFG defaultConstructor = createDefaultConstructor(cUnit);
			fixConstructorCFG(defaultConstructor, new FieldDeclaration[0]);
		}

		return false;
	}

	@Override
	public boolean visit(
			EnumDeclaration node) {
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(fullName);

		// build the enum constructor (for initializing fields)
		createEnumConstructor(enUnit);

		// build enum static initializer
		createEnumInitializer(enUnit);
		return false;
	}

	@Override
	public boolean visit(
			TypeDeclaration node) {
		// parsing superclass
		ClassUnit cUnit = (ClassUnit) getProgram().getUnit(fullName);

		if (!node.permittedTypes().isEmpty())
			throw new ParsingException("permits", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Permits is not supported.", getSourceCodeLocation(node));

		createClassInitializer(cUnit, node);

		// for (MethodDeclaration md : node.getMethods()) {
		// MethodASTVisitor visitor = new MethodASTVisitor(parserContext,
		// source, cUnit, compilationUnit, true, this,
		// enclosingType);
		// md.accept(visitor);
		// }

		boolean createDefaultConstructor = true;
		for (MethodDeclaration md : node.getMethods()) {
			MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit, false, this,
					enclosingType);
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
		String simpleName = enumUnit.getName().contains(".")
				? enumUnit.getName().substring(enumUnit.getName().lastIndexOf(".") + 1)
				: enumUnit.getName();
		CodeMemberDescriptor cmDesc = new CodeMemberDescriptor(
				locationManager.nextLocation(),
				enumUnit,
				false,
				simpleName + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Annotations(),
				new Parameter[0]);
		CFG cfg = new CFG(cmDesc);

		// in the main method, we instantiate enum constants
		it.unive.lisa.type.Type enumType = getProgram().getTypes().getType(enumUnit.getName());

		Statement init = null, last = null;
		for (Global target : enumUnit.getGlobals()) {
			JavaAccessGlobal accessGlobal = new JavaAccessGlobal(cfg, locationManager.nextLocation(), enumUnit, target);
			JavaNewObj call = new JavaNewObj(cfg, locationManager.nextLocation(),
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
		String simpleName = enumUnit.getName().contains(".")
				? enumUnit.getName().substring(enumUnit.getName().lastIndexOf(".") + 1)
				: enumUnit.getName();
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(locationManager.nextLocation(), enumUnit,
				true, simpleName, VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, new VariableInfo("this", null), new JavaReferenceType(type));
		parserContext.addVariableType(cfg, new VariableInfo("name", null),
				new JavaReferenceType(getProgram().getTypes().getStringType()));

		JavaAssignment glAsg = new JavaAssignment(cfg, locationManager.nextLocation(),
				new JavaAccessInstanceGlobal(cfg, locationManager.nextLocation(),
						new VariableRef(cfg, locationManager.nextLocation(), "this", new JavaReferenceType(type)),
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
		String simpleName = unit.getName().contains(".")
				? unit.getName().substring(unit.getName().lastIndexOf(".") + 1)
				: unit.getName();
		CodeMemberDescriptor cmDesc = new CodeMemberDescriptor(
				locationManager.nextLocation(),
				unit,
				false,
				simpleName + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Annotations(),
				new Parameter[0]);
		CFG cfg = new CFG(cmDesc);

		// first, we add the clinit call to the superclass
		// TODO this might also retrieve interfaces defined in the txts
		// we have to fix interfaces and replace SingleHierarchyTraversal
		Set<it.unive.lisa.program.CompilationUnit> superClasses = unit
				.getImmediateAncestors().stream()
				.filter(u -> u instanceof ClassUnit)
				.collect(Collectors.toSet());

		// we can safely suppose that there exist a single superclass
		ClassUnit superClass = (ClassUnit) superClasses.stream().findFirst().get();
		String superSimpleName = superClass.getName().contains(".")
				? superClass.getName().substring(superClass.getName().lastIndexOf(".") + 1)
				: superClass.getName();
		JavaUnresolvedStaticCall superClassInit = new JavaUnresolvedStaticCall(
				cfg,
				locationManager.nextLocation(),
				superClass.toString(),
				superSimpleName + InitializedClassSet.SUFFIX_CLINIT,
				new Expression[0]);

		cfg.addNode(superClassInit);
		cfg.getEntrypoints().add(superClassInit);

		Statement last = superClassInit;

		// just static fields are considered to build the class initializer
		for (FieldDeclaration fd : staticFields) {
			TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit, this);
			fd.getType().accept(typeVisitor);
			Type type = typeVisitor.getType();
			if (type.isInMemoryType())
				type = new JavaReferenceType(type);

			for (Object f : fd.fragments()) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
				type = typeVisitor.liftToArray(type, fragment);

				it.unive.lisa.program.cfg.statement.Expression init;
				if (fragment.getInitializer() != null) {
					ExpressionVisitor exprVisitor = new ExpressionVisitor(
							parserContext,
							source,
							compilationUnit,
							cfg,
							null,
							this);
					fragment.getInitializer().accept(exprVisitor);
					init = exprVisitor.getExpression();
				} else
					init = type.defaultValue(cfg, locationManager.nextLocation());

				Global global = new Global(
						locationManager.nextLocation(),
						unit,
						fragment.getName().getIdentifier(),
						false,
						type,
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

		if (enclosingType != null)
			parameters.add(new Parameter(locationManager.nextLocation(), "$enclosing", enclosingType.getReference(),
					null, new Annotations()));

		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		String simpleName = classUnit.getName().contains(".")
				? classUnit.getName().substring(classUnit.getName().lastIndexOf(".") + 1)
				: classUnit.getName();
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(locationManager.nextLocation(), classUnit,
				true, simpleName, VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, new VariableInfo("this", null), new JavaReferenceType(type));
		// we filter just the class unit, not interfaces
		String superClassName = classUnit.getImmediateAncestors().stream().filter(s -> s instanceof ClassUnit)
				.findFirst().get().getName();
		String superClassSimpleName = superClassName.contains(".")
				? superClassName.substring(superClassName.lastIndexOf(".") + 1)
				: superClassName;

		JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, locationManager.nextLocation(), Call.CallType.INSTANCE,
				superClassName, superClassSimpleName, new VariableRef(cfg, locationManager.nextLocation(), "this"));

		Ret ret = new Ret(cfg, locationManager.nextLocation());
		cfg.addNode(ret);
		cfg.addNode(call);
		Statement last = call;

		if (enclosingType != null) {
			JavaAssignment asg = new JavaAssignment(
					cfg,
					locationManager.nextLocation(),
					new JavaAccessInstanceGlobal(cfg,
							locationManager.nextLocation(),
							new VariableRef(
									cfg,
									locationManager.nextLocation(),
									"this",
									new JavaReferenceType(type)),
							"$enclosing"),
					new VariableRef(
							cfg,
							locationManager.nextLocation(),
							"$enclosing",
							enclosingType.getReference()));
			cfg.addNode(asg);
			cfg.addEdge(new SequentialEdge(call, asg));
			last = asg;
		}

		cfg.addEdge(new SequentialEdge(last, ret));
		cfg.getEntrypoints().add(call);
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
		String ancestorSimpleName = ancestor.getName().contains(".")
				? ancestor.getName().substring(ancestor.getName().lastIndexOf(".") + 1)
				: ancestor.getName();
		boolean implicitlyCallSuper = true;
		if (injectionPoint instanceof JavaUnresolvedCall call) {
			if (ancestor.getName().equals(call.getQualifier()) && ancestorSimpleName.equals(call.getTargetName())) {
				implicitlyCallSuper = false;
				List<Edge> outEdges = new ArrayList<>(cfg.getNodeList().getOutgoingEdges(injectionPoint));
				if (outEdges.size() == 1) {
					injectionPoint = outEdges.getFirst().getDestination();
				}
			}
		}

		if (injectionPoint instanceof JavaUnresolvedCall &&
				((JavaUnresolvedCall) injectionPoint).getTargetName().equals(cfg.getDescriptor().getName())) {
			return;
		}
		if (implicitlyCallSuper) {
			// add a super() call to this constructor, as a first statement,
			// before the field initializator.
			SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
			JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, locationManager.nextLocation(),
					Call.CallType.INSTANCE, ancestor.getName(), ancestorSimpleName,
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
					compilationUnit, cfg, this);
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