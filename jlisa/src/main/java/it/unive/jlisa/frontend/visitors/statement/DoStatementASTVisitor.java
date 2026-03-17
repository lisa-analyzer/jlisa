package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.cfg.controlflow.loops.DoWhileLoop;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.DoStatement;

class DoStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private static final Logger LOG = LogManager.getLogger(DoStatementASTVisitor.class);

	private ParsedBlock block;

	DoStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			DoStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		getScope().getTracker().enterScope();

		if (node.getBody() == null)
			return false; // parsing error

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		Statement entry;

		block.mergeWith(loopBody.getBody());
		entry = loopBody.getBegin();

		Expression expression = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Statement noop = new NoOp(getScope().getCFG(), expression.getLocation());

		boolean isConditionDeadcode = !loopBody.canBeContinued();

		if (!isConditionDeadcode) {
			block.addNode(expression);
			block.addEdge(new SequentialEdge(loopBody.getEnd(), expression));

			block.addEdge(new TrueEdge(expression, loopBody.getBegin()));

			block.addNode(noop);
			block.addEdge(new FalseEdge(expression, noop));

			// TODO: labels
			DoWhileLoop doWhileLoop = new DoWhileLoop(block, expression, noop, loopBody.getBody().getNodes());
			getScope().getCFG().getDescriptor().addControlFlowStructure(doWhileLoop);
			getScope().getControlFlowTracker().endControlFlowOf(block, expression, noop, expression, null);
		} else {
			LOG.warn("The last statement of do-while's body stops the execution, then the guard is not reachable.");
			getScope().getControlFlowTracker().getModifiers()
					.removeIf(pair -> pair.getRight() == null
							&& (pair.getLeft().continuesControlFlow() || pair.getLeft().breaksControlFlow()));
		}

		this.block = new ParsedBlock(entry, block, isConditionDeadcode ? loopBody.getEnd() : noop);
		getScope().getTracker().exitScope(noop);

		return false;
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
