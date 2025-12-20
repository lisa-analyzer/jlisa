package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.annotations.AnnotationInfo;
import it.unive.jlisa.frontend.annotations.MethodAnnotationExtractor;
import it.unive.jlisa.frontend.exceptions.JavaSyntaxException;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.JavaCFGTweaker;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.*;

public class MethodASTVisitor extends BaseCodeElementASTVisitor {
	it.unive.lisa.program.CompilationUnit lisacompilationUnit;
	CFG cfg;

	public MethodASTVisitor(
			ParserContext parserContext,
			String source,
			it.unive.lisa.program.CompilationUnit lisacompilationUnit,
			CompilationUnit astCompilationUnit,
			BaseUnitASTVisitor container) {
		super(parserContext, source, astCompilationUnit, container);
		this.lisacompilationUnit = lisacompilationUnit;
	}

	@Override
	public boolean visit(
			MethodDeclaration node) {
		JavaCodeMemberDescriptor codeMemberDescriptor;
		if (node.isConstructor()) {
			codeMemberDescriptor = buildConstructorJavaCodeMemberDescriptor(node);
		} else {
			codeMemberDescriptor = buildJavaCodeMemberDescriptor(node);
		}
		// Annotations are already set in buildJavaCodeMemberDescriptor/buildConstructorJavaCodeMemberDescriptor

		boolean isMain = isMain(node);

		int modifiers = node.getModifiers();

		// Debug: check if descriptor is the same object before and after CFG creation
		try {
			java.io.FileWriter fw = new java.io.FileWriter("descriptor-debug.log", true);
			fw.write("[DESC_ORIG] class=" + codeMemberDescriptor.getClass().getName()
					+ " id=" + System.identityHashCode(codeMemberDescriptor)
					+ " fullSig=" + codeMemberDescriptor.getFullSignature()
					+ " anns=" + codeMemberDescriptor.getAnnotations() + "\n");
			fw.close();
		} catch (Exception e) {
		}

		this.cfg = new CFG(codeMemberDescriptor);

		// Debug: check if descriptor inside CFG is the same object
		try {
			java.io.FileWriter fw = new java.io.FileWriter("descriptor-debug.log", true);
			fw.write("[DESC_CFG ] class=" + cfg.getDescriptor().getClass().getName()
					+ " id=" + System.identityHashCode(cfg.getDescriptor())
					+ " fullSig=" + cfg.getDescriptor().getFullSignature()
					+ " anns=" + cfg.getDescriptor().getAnnotations() + "\n");
			fw.close();
		} catch (Exception e) {
		}
		for (Parameter p : codeMemberDescriptor.getFormals()) {
			it.unive.lisa.type.Type paramType = p.getStaticType();
			parserContext.addVariableType(cfg, new VariableInfo(p.getName(), null),
					paramType.isInMemoryType() ? new JavaReferenceType(paramType) : paramType);
		}

		JavaLocalVariableTracker tracker = new JavaLocalVariableTracker(cfg, codeMemberDescriptor);
		tracker.enterScope();
		Parameter[] formalParams = codeMemberDescriptor.getFormals();
		for (int i = 0; i < formalParams.length - 1; i++) {
			for (int j = i + 1; j < formalParams.length; j++) {
				if (formalParams[i].getName().equals(formalParams[j].getName()))
					throw new ParsingException("parameter-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
							"Parameter " + formalParams[j].getName() + " already exists in the cfg",
							getSourceCodeLocation(node));
			}
		}

		for (Parameter p : formalParams) {
			parserContext.addVariableType(cfg, new VariableInfo(p.getName(), null), p.getStaticType());
			// Not required add the parameter in the tracker because it is done
			// in the tracker constructor given the descriptor.
		}

		BlockStatementASTVisitor blockStatementASTVisitor = new BlockStatementASTVisitor(parserContext, source,
				compilationUnit, cfg, tracker, container);

		if (node.getBody() == null) // e.g. abstract method declarations
			return false;

		node.getBody().accept(blockStatementASTVisitor);

		cfg.getNodeList().mergeWith(blockStatementASTVisitor.getBlock().getBody());
		if (blockStatementASTVisitor.getBlock().getBody().getNodes().isEmpty()) {

		}

		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		Collection<Statement> entrypoints = cfg.getEntrypoints();
		Statement first = blockStatementASTVisitor.getFirst();

		entrypoints.add(first);

		List<AnnotationInfo> anns = parserContext.getMethodAnnotations(cfg.getDescriptor());
		for (AnnotationInfo ann : anns) {
			if ("GetMapping".equals(ann.getName())) {
				getProgram().addEntryPoint(cfg);
				break;
			}
		}

		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg, parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
			if (cfg.getNodesCount() == 0) {
				// empty method, so the ret is also the entrypoint
				list.addNode(ret);
				entrypoints.add(ret);
			} else {
				// every non-throwing instruction that does not have a follower
				// is ending the method
				Collection<Statement> preExits = new LinkedList<>();
				for (Statement st : list.getNodes())
					if (!st.stopsExecution() && list.followersOf(st).isEmpty())
						preExits.add(st);
				list.addNode(ret);
				for (Statement st : preExits)
					list.addEdge(new SequentialEdge(st, ret));

				for (VariableTableEntry entry : cfg.getDescriptor().getVariables())
					if (preExits.contains(entry.getScopeEnd()))
						entry.setScopeEnd(ret);
			}

		}

		boolean added = false;

		if (!Modifier.isStatic(modifiers)) {
			added = lisacompilationUnit.addInstanceCodeMember(cfg);
		} else {
			added = lisacompilationUnit.addCodeMember(cfg);
		}
		// Use getFullSignatureWithParNames() to get the signature with annotations (same as HTML uses)
		// This avoids duplicate annotations that would appear if we used toString() which also includes annotations
		System.out.println("[ADDED] " + cfg.getDescriptor().getFullSignatureWithParNames() + " -> " + added);

		if (!added)
			throw new ParsingException("duplicated_method_descriptor",
					ParsingException.Type.MALFORMED_SOURCE,
					"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + lisacompilationUnit.getName(),
					getSourceCodeLocation(node));

		if (isMain)
			getProgram().addEntryPoint(cfg);

		JavaCFGTweaker.splitProtectedYields(cfg, JavaSyntaxException::new,
				parserContext.getCurrentSyntheticCodeLocationManager(source));
		JavaCFGTweaker.addFinallyEdges(cfg, JavaSyntaxException::new);
		JavaCFGTweaker.addReturns(cfg, JavaSyntaxException::new,
				parserContext.getCurrentSyntheticCodeLocationManager(source));
		cfg.simplify();

		tracker.exitScope(blockStatementASTVisitor.getLast());

		return false;
	}

	private JavaCodeMemberDescriptor buildJavaCodeMemberDescriptor(
			MethodDeclaration node) {
		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit, container);
		node.getReturnType2().accept(typeVisitor);

		it.unive.lisa.type.Type returnType = typeVisitor.getType();
		List<Parameter> parameters = new ArrayList<Parameter>();
		if (instance) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());
			parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
					new Annotations()));
		}

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source,
					compilationUnit, container);
			sd.accept(vd);
			parameters.add(vd.getParameter());
		}

		// Extract and add annotations to the Annotations object BEFORE creating the descriptor
		// This is the official way LiSA displays annotations in HTML
		Annotations annotations = new Annotations();
		fillCodeMemberAnnotations(node, annotations);
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisacompilationUnit, instance,
				node.getName().getIdentifier(),
				returnType.isInMemoryType() ? new JavaReferenceType(returnType) : returnType, annotations, paramArray);
		if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
			codeMemberDescriptor.setOverridable(false);
		} else {
			codeMemberDescriptor.setOverridable(true);
		}

		// Register annotations in ParserContext for other uses (not for HTML)
		// HTML uses the Annotations object passed to the constructor above
		MethodAnnotationExtractor.detectAndRegisterGetMapping(this.parserContext,
				codeMemberDescriptor, node);

		// Debug: check if annotations are in the descriptor
		try {
			java.io.FileWriter fw = new java.io.FileWriter("annotation-fill.log", true);
			fw.write("[DBG] member anns = " + codeMemberDescriptor.getAnnotations() + "\n");
			fw.write("[DBG] full sig    = " + codeMemberDescriptor.getFullSignature() + "\n");
			fw.write("[DBG] toString    = " + codeMemberDescriptor.toString() + "\n");
			fw.close();
		} catch (Exception e) {
		}

		return codeMemberDescriptor;
	}

	private JavaCodeMemberDescriptor buildConstructorJavaCodeMemberDescriptor(
			MethodDeclaration node) {

		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());
		it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
				new Annotations()));
		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source,
					compilationUnit, container);
			sd.accept(vd);
			parameters.add(vd.getParameter());
		}

		// Extract and add annotations to the Annotations object BEFORE creating the descriptor
		// This is the official way LiSA displays annotations in HTML
		Annotations annotations = new Annotations();
		fillCodeMemberAnnotations(node, annotations);
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisacompilationUnit, instance,
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
		Type type = parameter.getType();
		if (parameter.getType().toString().equals("String[]")) {
			return true;
		}
		if (type instanceof SimpleType && ((SimpleType) type).getName().toString().equals("String")
				&& parameter.getExtraDimensions() == 1) {
			return true;
		}

		return false;
	}

	public CFG getCFG() {
		return this.cfg;
	}

	private void fillCodeMemberAnnotations(
			MethodDeclaration node,
			Annotations target) {
		try {
			java.io.FileWriter fw = new java.io.FileWriter("annotation-fill.log", true);
			fw.write("[FILL] Processing method: " + node.getName().getIdentifier() + "\n");
			fw.write("[FILL] Modifiers count: " + node.modifiers().size() + "\n");
			fw.close();
		} catch (Exception e) {
		}

		for (Object m : node.modifiers()) {
			if (!(m instanceof org.eclipse.jdt.core.dom.Annotation ann))
				continue;

			// name annotation (without package)
			String simple = ann.getTypeName().getFullyQualifiedName();
			int dot = simple.lastIndexOf('.');
			if (dot >= 0)
				simple = simple.substring(dot + 1);

			try {
				java.io.FileWriter fw = new java.io.FileWriter("annotation-fill.log", true);
				fw.write("[FILL] Found annotation: " + simple + "\n");
				fw.close();
			} catch (Exception e) {
			}

			// Only add Spring mapping annotations (GetMapping, PostMapping, etc.)
			// This is what LiSA uses to display annotations in HTML
			if (!isSpringEndpointAnnotation(simple)) {
				try {
					java.io.FileWriter fw = new java.io.FileWriter("annotation-fill.log", true);
					fw.write("[FILL] Skipping annotation (not Spring endpoint): " + simple + "\n");
					fw.close();
				} catch (Exception e) {
				}
				continue;
			}

			// "Annotation" related to LiSA (not JDT)
			// For now, just add the annotation name - LiSA will display it in HTML
			try {
				java.io.FileWriter fw = new java.io.FileWriter("annotation-fill.log", true);
				fw.write("[FILL] Adding annotation: " + simple + " for method: " + node.getName().getIdentifier() + "\n");
				fw.close();
			} catch (Exception e) {
			}
			target.addAnnotation(new it.unive.lisa.program.annotations.Annotation(simple));
		}
	}


	private static boolean isSpringEndpointAnnotation(
			String simple) {
		return "GetMapping".equals(simple)
				|| "PostMapping".equals(simple)
				|| "PutMapping".equals(simple)
				|| "DeleteMapping".equals(simple)
				|| "PatchMapping".equals(simple)
				|| "RequestMapping".equals(simple);
	}

	private String extractSpringMappingPath(
			org.eclipse.jdt.core.dom.Annotation ann) {
		// @GetMapping("/x") (SingleMemberAnnotation)
		if (ann instanceof org.eclipse.jdt.core.dom.SingleMemberAnnotation sma) {
			String raw = sma.getValue().toString();
			return normalizePathValue(raw);
		}

		// @GetMapping(value="/x") or @GetMapping(path="/x") (NormalAnnotation)
		if (ann instanceof org.eclipse.jdt.core.dom.NormalAnnotation na) {
			for (Object o : na.values()) {
				org.eclipse.jdt.core.dom.MemberValuePair p = (org.eclipse.jdt.core.dom.MemberValuePair) o;
				String key = p.getName().getIdentifier();
				if (!"value".equals(key) && !"path".equals(key))
					continue;

				String raw = p.getValue().toString();
				return normalizePathValue(raw);
			}
		}

		return null;
	}

	private String normalizePathValue(
			String raw) {
		if (raw == null)
			return null;

		raw = raw.trim();

		// {"a","b"} -> take first
		if (raw.startsWith("{") && raw.endsWith("}")) {
			raw = raw.substring(1, raw.length() - 1).trim();
			int comma = raw.indexOf(',');
			if (comma != -1)
				raw = raw.substring(0, comma).trim();
		}

		// remove quotes if present
		raw = stripQuotes(raw);

		return raw;
	}

	private static String stripQuotes(
			String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))))
			return s.substring(1, s.length() - 1);
		return s;
	}

}
