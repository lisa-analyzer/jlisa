package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
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

    public BlockStatementASTVisitor(ParserContext parserContext, String source,CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    public BlockStatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        this(parserContext, source, compilationUnit);
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
        if (node.statements().isEmpty()) {
            first = new NoOp(cfg, getSourceCodeLocation(node));
            last = new NoOp(cfg, getSourceCodeLocation(node));
            block.addNode(first);
        }
        for (Object o : node.statements()) {
            StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg);
            ((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);
            block.mergeWith(statementASTVisitor.getBlock());
            if (statementASTVisitor.getBlock().getNodes().isEmpty()) {
                // parsing error.
                return false;
            }
            if (first == null) {
                first = statementASTVisitor.getFirst();
            }
            if (last != null) {
                block.addEdge(new SequentialEdge(last, statementASTVisitor.getFirst()));
            }
            last = statementASTVisitor.getLast();
        }
        return false;
    }
}
