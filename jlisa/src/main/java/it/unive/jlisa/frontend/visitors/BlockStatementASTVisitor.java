package it.unive.jlisa.frontend.visitors;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.Collection;
import java.util.HashSet;

public class BlockStatementASTVisitor extends JavaASTVisitor{
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Statement first;
    private it.unive.lisa.program.cfg.statement.Statement last;
    private NodeList<CFG, it.unive.lisa.program.cfg.statement.Statement, Edge> block = new NodeList<>(new SequentialEdge());

    public BlockStatementASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit) {
        super(program, source, apiLevel, compilationUnit);
    }

    public BlockStatementASTVisitor(Program program, String source, int apiLevel, CompilationUnit compilationUnit, CFG cfg) {
        this(program, source, apiLevel, compilationUnit);
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

    public boolean visit(Block node) {
        block = new NodeList<>(new SequentialEdge());

        for (Object o : node.statements()) {
            StatementASTVisitor statementASTVisitor = new StatementASTVisitor(program, source, apiLevel, compilationUnit, cfg);
            ((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);
            block.mergeWith(statementASTVisitor.getBlock());
            if (first == null) {
                first = statementASTVisitor.getFirst();
            }
            if (last != null) {
                block.addEdge(new SequentialEdge(last, statementASTVisitor.getLast()));
            }
            last = statementASTVisitor.getLast();
        }
        return false;
    }
}
