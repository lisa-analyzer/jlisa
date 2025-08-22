package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.program.cfg.expression.instrumentations.EmptyBody;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import it.unive.lisa.util.frontend.ParsedBlock;

public class BlockStatementASTVisitor extends JavaASTVisitor{
    private CFG cfg;
    private ParsedBlock block;

    public BlockStatementASTVisitor(ParserContext parserContext, String source,CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    public BlockStatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        this(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    public Statement getFirst() {
        return block.getBegin();
    }

    public Statement getLast() {
        return block.getEnd();
    }

    public ParsedBlock getBlock() {
        return block;
    }

    public boolean visit(Block node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());

		Statement first = null, last = null;
		if(node.statements().isEmpty()) { // empty block
			EmptyBody emptyBlock = null;
			emptyBlock = new EmptyBody(cfg, getSourceCodeLocation(node));
			nodeList.addNode(emptyBlock);
			last = emptyBlock;
			nodeList.addEdge(new SequentialEdge(last, emptyBlock));

		} else {
			for (Object o : node.statements()) {
				StatementASTVisitor stmtVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg, new ControlFlowTracker());
				((org.eclipse.jdt.core.dom.Statement) o).accept(stmtVisitor);
				
				ParsedBlock stmtBlock = stmtVisitor.getBlock();
				
				nodeList.mergeWith(stmtBlock.getBody());
				
				if (first == null)
					first = stmtBlock.getBegin();
	
				if (last != null)
					nodeList.addEdge(new SequentialEdge(last, stmtBlock.getBegin()));

				last = stmtBlock.getEnd();
			}
		}
		
		this.block = new ParsedBlock(first, nodeList, last);
		return false;
    }
}
