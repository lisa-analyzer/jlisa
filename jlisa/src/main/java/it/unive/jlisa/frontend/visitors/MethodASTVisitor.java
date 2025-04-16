package it.unive.jlisa.frontend.visitors;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class MethodASTVisitor extends JavaASTVisitor {
    it.unive.lisa.program.CompilationUnit lisacompilationUnit;

    private MethodASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }

    public MethodASTVisitor(Program program, String source, int apiLevel, it.unive.lisa.program.CompilationUnit lisacompilationUnit, CompilationUnit astCompilationUnit) {
        this(program, source, apiLevel, astCompilationUnit);
        this.lisacompilationUnit = lisacompilationUnit;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.isConstructor()) {
            return false; // TODO
        }
        boolean isMain = isMain(node);

        int modifiers = node.getModifiers();
        SourceCodeLocation loc = getSourceCodeLocation(node);
        CodeMemberDescriptor codeMemberDescriptor = buildCodeMemberDescriptor(node);
        CFG cfg = new CFG(codeMemberDescriptor);
        BlockStatementASTVisitor blockStatementASTVisitor = new BlockStatementASTVisitor(program, source, apiLevel, compilationUnit, cfg);
        node.getBody().accept(blockStatementASTVisitor);
        cfg.getNodeList().mergeWith(blockStatementASTVisitor.getBlock());
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
                Collection<it.unive.lisa.program.cfg.statement.Statement> preExits = new LinkedList<>();
                for (it.unive.lisa.program.cfg.statement.Statement st : list.getNodes())
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
        lisacompilationUnit.addInstanceCodeMember(cfg);
        //compilationUnit.addInstanceCodeMember()
        if (isMain) {
            program.addEntryPoint(cfg);
        }
        return super.visit(node);
    }

    private CodeMemberDescriptor buildCodeMemberDescriptor(MethodDeclaration node) {
        CodeLocation loc = getSourceCodeLocation(node);
        CodeMemberDescriptor codeMemberDescriptor;
        boolean instance = !Modifier.isStatic(node.getModifiers());
        TypeASTVisitor typeVisitor = new TypeASTVisitor(program, source, apiLevel, compilationUnit);
        node.getReturnType2().accept(typeVisitor);

        it.unive.lisa.type.Type returnType = typeVisitor.getType();
        List<Parameter> parameters = new ArrayList<Parameter>();
        for (Object o : node.parameters()) {
            SingleVariableDeclaration sd = (SingleVariableDeclaration) o;
            VariableDeclarationASTVisitor vd = new VariableDeclarationASTVisitor(program, source, apiLevel, compilationUnit);
            sd.accept(vd);
            parameters.add(vd.getParameter());

        }
        //TODO annotations
        Annotations annotations = new Annotations();
        Parameter[] paramArray = parameters.toArray(new Parameter[0]);
        codeMemberDescriptor = new CodeMemberDescriptor(loc, lisacompilationUnit, instance, node.getName().getIdentifier(), returnType, annotations, paramArray);
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
}
