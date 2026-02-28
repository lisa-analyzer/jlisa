package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.JavaSyntaxException;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.JavaCFGTweaker;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.scope.ClassScope;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

class MethodASTVisitor extends ScopedVisitor<ClassScope> implements ResultHolder<CFG> {
	private CFG cfg;

	public MethodASTVisitor(
			ParsingEnvironment environment,
			ClassScope scope) {
		super(environment, scope);
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

		CodeMember codeMember;
		if (!Modifier.isStatic(modifiers)) {
			codeMember = getScope().getLisaClassUnit().getInstanceCodeMember(codeMemberDescriptor.getSignature(),
					false);
		} else {
			codeMember = getScope().getLisaClassUnit().getCodeMember(codeMemberDescriptor.getSignature());
		}

		if (codeMember == null) {
			// should never happen.
			throw new ParsingException("missing_method_descriptor",
					ParsingException.Type.PARSING_ERROR,
					"Missing code member descriptor for " + codeMemberDescriptor + " in unit "
							+ getScope().getLisaClassUnit().getName(),
					getSourceCodeLocation(node));
		}

		cfg = (CFG) codeMember; // this explicit cast should always be possible.
		for (Parameter p : codeMemberDescriptor.getFormals()) {
			it.unive.lisa.type.Type paramType = p.getStaticType();
			getParserContext().addVariableType(cfg, new VariableInfo(p.getName(), null),
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
			getParserContext().addVariableType(cfg, new VariableInfo(p.getName(), null), p.getStaticType());
			// Not required add the parameter in the tracker because it is done
			// in the tracker constructor given the descriptor.
		}

		if (node.getBody() == null) // e.g. abstract method declarations
			return false;

		ParsedBlock block = getParserContext().evaluate(
				node.getBody(),
				() -> new BlockStatementASTVisitor(getEnvironment(),
						getScope().toMethodScope(cfg, tracker, new ControlFlowTracker())));

		cfg.getNodeList().mergeWith(block.getBody());

		if (node.isConstructor() && getScope().getEnclosingClass() != null) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(getScope().getLisaClassUnit().getName());
			JavaAssignment asg = new JavaAssignment(
					cfg,
					getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
					new JavaAccessInstanceGlobal(cfg,
							getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
							new VariableRef(
									cfg,
									getParserContext().getCurrentSyntheticCodeLocationManager(getSource())
											.nextLocation(),
									"this",
									new JavaReferenceType(type)),
							"$enclosing"),
					new VariableRef(
							cfg,
							getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
							"$enclosing",
							getScope().getEnclosingClass().getReference()));
			cfg.addNode(asg);
			cfg.getEntrypoints().add(asg);
			cfg.addEdge(new SequentialEdge(asg, block.getBegin()));
		}

		if (block.getBody().getNodes().isEmpty()) {
			return false;
		}

		if (!node.isConstructor() || getScope().getEnclosingClass() == null)
			cfg.getEntrypoints().add(block.getBegin());
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		Collection<Statement> entrypoints = cfg.getEntrypoints();
		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg,
					getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
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
				getParserContext().getCurrentSyntheticCodeLocationManager(getSource()));
		JavaCFGTweaker.addFinallyEdges(cfg, JavaSyntaxException::new);
		JavaCFGTweaker.addReturns(cfg, JavaSyntaxException::new,
				getParserContext().getCurrentSyntheticCodeLocationManager(getSource()));
		cfg.simplify();

		tracker.exitScope(block.getEnd());

		return false;
	}

	@SuppressWarnings("unchecked")
	private JavaCodeMemberDescriptor buildJavaCodeMemberDescriptor(
			MethodDeclaration node) {
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
				TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getUnitScope());
				node.getReturnType2().accept(typeVisitor);
				returnType = typeVisitor.getType();
			}
		}

		List<Parameter> parameters = new ArrayList<Parameter>();
		if (instance) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(getScope().getLisaClassUnit().getName());
			parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
					new Annotations()));
		}

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(getEnvironment(),
					getScope().getUnitScope());
			sd.accept(vd);
			parameters.add(vd.getResult());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, getScope().getLisaClassUnit(), instance,
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
		it.unive.lisa.type.Type type = getProgram().getTypes().getType(getScope().getLisaClassUnit().getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new JavaReferenceType(type), null,
				new Annotations()));

		if (getScope().getEnclosingClass() != null)
			parameters
					.add(new Parameter(getSourceCodeLocationManager(node).nextColumn(), "$enclosing",
							getScope().getEnclosingClass().getReference(),
							null, new Annotations()));

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(getEnvironment(),
					getScope().getUnitScope());
			sd.accept(vd);
			parameters.add(vd.getResult());
		}

		// TODO annotations
		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, getScope().getLisaClassUnit(), instance,
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

	@Override
	public CFG getResult() {
		return cfg;
	}
}
