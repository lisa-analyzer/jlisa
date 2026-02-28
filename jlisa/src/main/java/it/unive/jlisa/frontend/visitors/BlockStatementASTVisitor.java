package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import org.eclipse.jdt.core.dom.Block;

class BlockStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {
	private ParsedBlock block;

	public BlockStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	public Statement getFirst() {
		return block.getBegin();
	}

	public Statement getLast() {
		return block.getEnd();
	}

	public boolean visit(
			Block node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());

		Statement first = null, last = null;
		if (node.statements().isEmpty()) { // empty block
			NoOp emptyBlock = new NoOp(getScope().getCFG(), getSourceCodeLocation(node));
			nodeList.addNode(emptyBlock);
			first = emptyBlock;
			last = emptyBlock;
		} else {
			for (Object o : node.statements()) {
				StatementASTVisitor stmtVisitor = new StatementASTVisitor(getEnvironment(), getScope());
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

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
