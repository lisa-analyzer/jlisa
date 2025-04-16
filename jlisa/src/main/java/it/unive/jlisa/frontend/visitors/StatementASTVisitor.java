package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.*;

public class StatementASTVisitor extends JavaASTVisitor {
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Statement first;
    private it.unive.lisa.program.cfg.statement.Statement last;
    private NodeList<CFG, it.unive.lisa.program.cfg.statement.Statement, Edge> block = new NodeList<>(new SequentialEdge());

    public StatementASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }

    public StatementASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit, CFG cfg) {
        super(program, source, apiLevel, compilationUnit);
        this.cfg = cfg;
    }

    public Statement getFirst() {
        return first;
    }

    public Statement getLast() {
        return last;
    }

    public NodeList<CFG, Statement, Edge> getBlock() {
        return block;
    }

    @Override
    public boolean visit(AssertStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(Block node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(IfStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(SwitchStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        System.out.println(node);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        System.out.println(node);
        block = new NodeList<>(new SequentialEdge());
        TypeASTVisitor visitor = new TypeASTVisitor(program, source, apiLevel, compilationUnit);
        node.getType().accept(visitor);
        Type variableType = visitor.getType();
        for (Object f : node.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
            String variableName = fragment.getName().getIdentifier();
            SourceCodeLocation loc = getSourceCodeLocation(fragment);
            VariableRef ref = new VariableRef(cfg,
                    getSourceCodeLocation(fragment),
                    variableName, variableType);
            Expression initializer = new NullLiteral(cfg, loc);
            if(fragment.getInitializer() == null) {
                if (variableType == Int32Type.INSTANCE) {
                    initializer = new Int32Literal(cfg, loc, (int)JavaTypeSystem.getDefaultValue(variableType));
                } else {
                    initializer = new NullLiteral(cfg, loc);
                }
            }
            Assignment assignment = new Assignment(cfg, loc, ref, initializer);
            block.addNode(assignment);
            if (first == null) {
                first = assignment;
            } else {
                block.addEdge(new SequentialEdge(first, assignment));
            }
            //cfg.getNodeList().mergeWith(block);
            //cfg.addNode(ref);
            last = assignment;
            //fragment.getInitializer()
        }
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        System.out.println(node);
        return false;
    }



}
