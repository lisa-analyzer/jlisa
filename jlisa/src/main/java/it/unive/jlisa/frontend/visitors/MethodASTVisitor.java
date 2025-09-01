package it.unive.jlisa.frontend.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.JavaSyntaxException;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.CFGTweaker;
import it.unive.lisa.util.frontend.LocalVariableTracker;

public class MethodASTVisitor extends JavaASTVisitor {
    it.unive.lisa.program.CompilationUnit lisacompilationUnit;
    CFG cfg;
    
    public MethodASTVisitor(ParserContext parserContext, String source, it.unive.lisa.program.CompilationUnit lisacompilationUnit, CompilationUnit astCompilationUnit) {
        super(parserContext, source, astCompilationUnit);
        this.lisacompilationUnit = lisacompilationUnit;
    }

	@Override
    public boolean visit(MethodDeclaration node) {
        JavaCodeMemberDescriptor codeMemberDescriptor;
        if (node.isConstructor()) {
            codeMemberDescriptor = buildConstructorJavaCodeMemberDescriptor(node);
        } else {
            codeMemberDescriptor = buildJavaCodeMemberDescriptor(node);
        }
        
        boolean isMain = isMain(node);
        
        int modifiers = node.getModifiers();
        this.cfg = new CFG(codeMemberDescriptor);
        
        LocalVariableTracker tracker = new LocalVariableTracker(cfg, codeMemberDescriptor);
        tracker.enterScope();
        Parameter[] formalParams = codeMemberDescriptor.getFormals();
        for (int i=0; i < formalParams.length-1; i++) {
        	for (int j=i; j < formalParams.length; j++) {
        		if(formalParams[i].getName().equals(formalParams[j].getName()))		
        			parserContext.addException(
        					new ParsingException("parameter-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
        							"Parameter " + formalParams[j].getName() + " already exists in the cfg", getSourceCodeLocation(node)));
        	}
        }
		
        for (Parameter p : formalParams) {
            parserContext.addVariableType(cfg, p.getName(), p.getStaticType()); //FIXME: to remove when changed the visibility of LocalVariableTracker fields
            // Not required add the parameter in the tracker because it is done in the tracker constructor given the descriptor.
        }
        
        
        BlockStatementASTVisitor blockStatementASTVisitor = new BlockStatementASTVisitor(parserContext, source, compilationUnit, cfg, tracker);
        node.getBody().accept(blockStatementASTVisitor);
        cfg.getNodeList().mergeWith(blockStatementASTVisitor.getBlock().getBody());
        if (blockStatementASTVisitor.getBlock().getBody().getNodes().isEmpty()) {
            return false;
        }
        cfg.getEntrypoints().add(blockStatementASTVisitor.getFirst());
        NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
        Collection<Statement> entrypoints = cfg.getEntrypoints();
        if (cfg.getAllExitpoints().isEmpty()) {
            Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
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
        if (!Modifier.isStatic(modifiers)) {
            added = lisacompilationUnit.addInstanceCodeMember(cfg);
        } else {
            added = lisacompilationUnit.addCodeMember(cfg);
        }
        
        if (!added) {
            parserContext.addException(new ParsingException("duplicated_method_descriptor",
                    ParsingException.Type.MALFORMED_SOURCE,
                    "Duplicate descriptor " + cfg.getDescriptor() + " in unit " + lisacompilationUnit.getName(),
                    getSourceCodeLocation(node)));
        }

		if (isMain) {
			// in the main method, we instantiate enum constants
			for (Unit unit : getProgram().getUnits()) 
				if (unit instanceof EnumUnit) {
					it.unive.lisa.type.Type enumType = getProgram().getTypes().getType(unit.getName());

					for (Global target : unit.getGlobals()) {
						int line = ((SourceCodeLocation) target.getLocation()).getLine();
						int col = ((SourceCodeLocation) target.getLocation()).getCol();
						JavaAccessGlobal accessGlobal = new JavaAccessGlobal(cfg, new SourceCodeLocation(source, line, col), unit, target);
						JavaNewObj call = new JavaNewObj(cfg, new SourceCodeLocation(source, line, col + 1), unit.getName(), new ReferenceType(enumType), new JavaStringLiteral(cfg, new SourceCodeLocation(source, line, col + 2), target.getName()));
						JavaAssignment asg = new JavaAssignment(cfg, new SourceCodeLocation(source, line, col + 3), accessGlobal, call);
						cfg.addNode(asg);
						for (Statement entry : cfg.getEntrypoints()) {
							cfg.addEdge(new SequentialEdge(asg, entry));
							cfg.getEntrypoints().remove(entry);
							cfg.getEntrypoints().add(asg);
						}
					}
				}

			getProgram().addEntryPoint(cfg);
		}

		CFGTweaker.splitProtectedYields(cfg, JavaSyntaxException::new);
		CFGTweaker.addFinallyEdges(cfg, JavaSyntaxException::new);
		CFGTweaker.addReturns(cfg, JavaSyntaxException::new);
		cfg.simplify();
        
        tracker.exitScope(blockStatementASTVisitor.getLast());

		return false;
	}

    private JavaCodeMemberDescriptor buildJavaCodeMemberDescriptor(MethodDeclaration node) {
        CodeLocation loc = getSourceCodeLocation(node);
        JavaCodeMemberDescriptor codeMemberDescriptor;
        boolean instance = !Modifier.isStatic(node.getModifiers());
        TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getReturnType2().accept(typeVisitor);

		it.unive.lisa.type.Type returnType = typeVisitor.getType();
		List<Parameter> parameters = new ArrayList<Parameter>();
		if (instance) {
			it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());
			parameters.add(new Parameter(getSourceCodeLocation(node), "this", new ReferenceType(type), null, new Annotations()));
		}

		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source, compilationUnit);
			sd.accept(vd);
			parameters.add(vd.getParameter());
        }
		
        //TODO annotations
        Annotations annotations = new Annotations();
        Parameter[] paramArray = parameters.toArray(new Parameter[0]);
        codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisacompilationUnit, instance, node.getName().getIdentifier(), returnType.isInMemoryType() ? new ReferenceType(returnType) : returnType, annotations, paramArray);
        if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
            codeMemberDescriptor.setOverridable(false);
        } else {
            codeMemberDescriptor.setOverridable(true);
        }

		return codeMemberDescriptor;
	}

    private JavaCodeMemberDescriptor buildConstructorJavaCodeMemberDescriptor(MethodDeclaration node) {

        CodeLocation loc = getSourceCodeLocation(node);
        JavaCodeMemberDescriptor codeMemberDescriptor;
        boolean instance = !Modifier.isStatic(node.getModifiers());
        it.unive.lisa.type.Type type = getProgram().getTypes().getType(lisacompilationUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter(getSourceCodeLocation(node), "this", new ReferenceType(type), null, new Annotations()));
		for (Object o : node.parameters()) {
			SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
			VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(parserContext, source, compilationUnit);
			sd.accept(vd);
			parameters.add(vd.getParameter());
        }
		
        //TODO annotations
        Annotations annotations = new Annotations();
        Parameter[] paramArray = parameters.toArray(new Parameter[0]);
        codeMemberDescriptor = new JavaCodeMemberDescriptor(loc, lisacompilationUnit, instance, node.getName().getIdentifier(), VoidType.INSTANCE, annotations, paramArray);
        if (node.isConstructor() || Modifier.isStatic(node.getModifiers())) {
            codeMemberDescriptor.setOverridable(false);
        } else {
            codeMemberDescriptor.setOverridable(true);
        }

		return codeMemberDescriptor;
	}

	private boolean isMain(MethodDeclaration node) {
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
		if (type instanceof SimpleType && ((SimpleType) type).getName().toString().equals("String") && parameter.getExtraDimensions() == 1) {
			return true;
		}

		return false;
	}

	public CFG getCFG() {
		return this.cfg;
	}
}
