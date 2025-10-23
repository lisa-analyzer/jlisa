package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.JavaSyntaxException;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.JavaCFGTweaker;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.*;

public class MethodASTVisitor extends BaseCodeElementASTVisitor {
	it.unive.lisa.program.CompilationUnit lisacompilationUnit;
	CFG cfg;
	boolean createMethodSignature;
	private final JavaClassType enclosing;

	public MethodASTVisitor(
			ParserContext parserContext,
			String source,
			it.unive.lisa.program.CompilationUnit lisacompilationUnit,
			CompilationUnit astCompilationUnit,
			boolean createMethodSignature,
			BaseUnitASTVisitor container,
			JavaClassType enclosing) {
		super(parserContext, source, astCompilationUnit, container);
		this.lisacompilationUnit = lisacompilationUnit;
		this.createMethodSignature = createMethodSignature;
		this.enclosing = enclosing;
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

		boolean isMain = isMain(node);

		int modifiers = node.getModifiers();

		if (createMethodSignature) {
			CFG cfg = new CFG(codeMemberDescriptor);
			boolean added;

			if (!Modifier.isStatic(modifiers)) {
				added = lisacompilationUnit.addInstanceCodeMember(cfg);
			} else {
				added = lisacompilationUnit.addCodeMember(cfg);
			}
			if (!added)
				throw new ParsingException("duplicated_method_descriptor",
						ParsingException.Type.MALFORMED_SOURCE,
						"Duplicate descriptor " + cfg.getDescriptor() + " in unit " + lisacompilationUnit.getName(),
						getSourceCodeLocation(node));

			if (isMain)
				getProgram().addEntryPoint(cfg);
			return false;
		}

		CodeMember codeMember;
		if (!Modifier.isStatic(modifiers)) {
			codeMember = lisacompilationUnit.getInstanceCodeMember(codeMemberDescriptor.getSignature(), false);
		} else {
			codeMember = lisacompilationUnit.getCodeMember(codeMemberDescriptor.getSignature());
		}

		if (codeMember == null) {
			// should never happen.
			throw new ParsingException("missing_method_descriptor",
					ParsingException.Type.PARSING_ERROR,
					"Missing code member descriptor for " + codeMemberDescriptor + " in unit "
							+ lisacompilationUnit.getName(),
					getSourceCodeLocation(node));
		}

		cfg = (CFG) codeMember; // this explicit cast should always be possible.
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

		if (node.isConstructor() && enclosing != null) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());
			JavaAssignment asg = new JavaAssignment(
					cfg, 
					parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(),
					new JavaAccessInstanceGlobal(cfg, 
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(),
						new VariableRef(
							cfg, 
							parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), 
							"this", 
							new JavaReferenceType(type)),
						"$enclosing"),
					new VariableRef(
						cfg, 
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), 
						"$enclosing", 
						enclosing.getReference()));
			cfg.addNode(asg);
			cfg.getEntrypoints().add(asg);
			cfg.addEdge(new SequentialEdge(asg, blockStatementASTVisitor.getBlock().getBegin()));
		}
		
		if (blockStatementASTVisitor.getBlock().getBody().getNodes().isEmpty()) {
			return false;
		}

		if (!node.isConstructor() || enclosing == null)
			cfg.getEntrypoints().add(blockStatementASTVisitor.getFirst());
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		Collection<Statement> entrypoints = cfg.getEntrypoints();
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

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisacompilationUnit, instance,
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
			MethodDeclaration node) {

		CodeLocation loc = getSourceCodeLocation(node);
		JavaCodeMemberDescriptor codeMemberDescriptor;
		boolean instance = !Modifier.isStatic(node.getModifiers());
		it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
				new Annotations()));

		if (enclosing != null) 
			parameters.add(new Parameter(getSourceCodeLocationManager(node).nextColumn(), "$enclosing", enclosing.getReference(),
					null, new Annotations()));

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source,
					compilationUnit, container);
			sd.accept(vd);
			parameters.add(vd.getParameter());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
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
}
