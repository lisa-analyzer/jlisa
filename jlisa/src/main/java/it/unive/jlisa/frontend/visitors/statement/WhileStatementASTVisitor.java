package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.cfg.controlflow.loops.WhileLoop;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import org.eclipse.jdt.core.dom.WhileStatement;

class WhileStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private ParsedBlock block;

	WhileStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			WhileStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		getScope().getTracker().enterScope();

		Expression expression = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		adj.addNode(expression);

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		adj.mergeWith(loopBody.getBody());

		adj.addEdge(new TrueEdge(expression, loopBody.getBegin()));
		if (loopBody.canBeContinued())
			adj.addEdge(new SequentialEdge(loopBody.getEnd(), expression));

		Statement noop = new NoOp(getScope().getCFG(), expression.getLocation());
		adj.addNode(noop);
		adj.addEdge(new FalseEdge(expression, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		WhileLoop whileLoop = new WhileLoop(adj, expression, noop, loopBody.getBody().getNodes());
		getScope().getCFG().getDescriptor().addControlFlowStructure(whileLoop);
		getScope().getControlFlowTracker().endControlFlowOf(adj, expression, noop, expression, null);
		this.block = new ParsedBlock(expression, adj, noop);

		return false;
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
