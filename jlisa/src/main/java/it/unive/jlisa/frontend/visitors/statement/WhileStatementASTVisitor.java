package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.LoopUnrolling;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.cfg.controlflow.loops.WhileLoop;
import it.unive.jlisa.program.cfg.expression.JavaUnboxBoolean;
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

		Expression item = JavaUnboxBoolean.ifBoxed(getParserContext().evaluate(node.getExpression(),
				new ExpressionVisitor(getEnvironment(), getScope())));
		Statement noop = new NoOp(getScope().getCFG(), item.getLocation());
		adj.addNode(noop);

		boolean canUnroll = LoopUnrolling.canUnroll(node);
		int UNROLL_FACTOR = 8;

		Statement first = null;
		Statement last = null;

		if (canUnroll) {
			for (int i = 0; i < UNROLL_FACTOR; i++) {

				ParsingEnvironment env = getEnvironment().withUnrollingCopy(i + 2);

				Expression condition = JavaUnboxBoolean.ifBoxed(getParserContext().evaluate(node.getExpression(),
						new ExpressionVisitor(env, getScope())));

				ParsedBlock loopBody = getParserContext().evaluate(
						node.getBody(),
						new StatementASTVisitor(env, getScope()));

				adj.addNode(condition);
				adj.mergeWith(loopBody.getBody());
				adj.addEdge(new TrueEdge(condition, loopBody.getBegin()));
				adj.addEdge(new FalseEdge(condition, noop));

				if (first == null)
					first = condition;
				if (last != null)
					adj.addEdge(new SequentialEdge(last, condition));

				if (!loopBody.canBeContinued()) {
					last = null;
					break;
				}
				last = loopBody.getEnd();
			}
		}

		// remaining loop

		Expression expression = JavaUnboxBoolean.ifBoxed(getParserContext().evaluate(node.getExpression(),
				new ExpressionVisitor(getEnvironment(), getScope())));

		adj.addNode(expression);
		if (last != null)
			adj.addEdge(new SequentialEdge(last, expression));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				new StatementASTVisitor(getEnvironment(), getScope()));

		adj.mergeWith(loopBody.getBody());

		adj.addEdge(new TrueEdge(expression, loopBody.getBegin()));
		if (loopBody.canBeContinued())
			adj.addEdge(new SequentialEdge(loopBody.getEnd(), expression));

		adj.addEdge(new FalseEdge(expression, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		WhileLoop whileLoop = new WhileLoop(adj, expression, noop, loopBody.getBody().getNodes());
		getScope().getCFG().getDescriptor().addControlFlowStructure(whileLoop);
		getScope().getControlFlowTracker().endControlFlowOf(adj, expression, noop, expression, null);
		this.block = new ParsedBlock(first != null ? first : expression, adj, noop);

		return false;
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}

}
