package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.loops.ForLoop;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ForStatement;

class ForStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private static final Logger LOG = LogManager.getLogger(ForStatementASTVisitor.class);

	private ParsedBlock block;

	ForStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(
			ForStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		getScope().getTracker().enterScope();

		ParsedBlock initializers = visitSequentialExpressions(node.initializers());

		boolean hasInitalizers = initializers.getBegin() != null && initializers.getEnd() != null;
		SyntheticCodeLocationManager syntheticLocationManager = getParserContext()
				.getCurrentSyntheticCodeLocationManager(getSource());
		NoOp noInit = new NoOp(getScope().getCFG(), syntheticLocationManager.nextLocation());
		Statement entry;
		if (hasInitalizers) {
			block.mergeWith(initializers.getBody());
			entry = initializers.getBegin();
		} else {
			block.addNode(noInit);
			entry = noInit;
		}

		Expression condition = node.getExpression() != null
				? getParserContext().evaluate(node.getExpression(),
						() -> new ExpressionVisitor(getEnvironment(), getScope()))
				: null;
		Statement alwaysTrue = new Equal(getScope().getCFG(), syntheticLocationManager.nextLocation(),
				new TrueLiteral(getScope().getCFG(), syntheticLocationManager.nextLocation()),
				new TrueLiteral(getScope().getCFG(), syntheticLocationManager.nextLocation()));

		boolean hasCondition = condition != null;

		if (hasCondition) {
			block.addNode(condition);
			block.addEdge(new SequentialEdge(hasInitalizers ? initializers.getEnd() : noInit, condition));
		} else {
			block.addNode(alwaysTrue);
			block.addEdge(new SequentialEdge(hasInitalizers ? initializers.getEnd() : noInit, alwaysTrue));
		}

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		block.mergeWith(loopBody.getBody());

		block.addEdge(new TrueEdge(hasCondition ? condition : alwaysTrue, loopBody.getBegin()));

		ParsedBlock updaters = visitSequentialExpressions(node.updaters());

		boolean hasUpdaters = updaters.getBegin() != null && updaters.getEnd() != null;

		boolean areUpdatersDeadcode = !loopBody.canBeContinued();

		if (!areUpdatersDeadcode)
			block.mergeWith(updaters.getBody());
		else if (hasUpdaters)
			LOG.warn("The last statement of for's body stops the execution, then updaters are not reachable.");

		Statement noop = new NoOp(getScope().getCFG(),
				hasCondition ? condition.getLocation() : syntheticLocationManager.nextLocation());
		block.addNode(noop);

		if (!areUpdatersDeadcode) {
			block.addEdge(new SequentialEdge(loopBody.getEnd(),
					hasUpdaters ? updaters.getBegin() : hasCondition ? condition : alwaysTrue));
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getEnd() : loopBody.getEnd(),
					hasCondition ? condition : alwaysTrue));
		}

		block.addEdge(new FalseEdge(hasCondition ? condition : alwaysTrue, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		ForLoop forloop = new ForLoop(block, hasInitalizers ? initializers.getBody().getNodes() : null,
				hasCondition ? condition : alwaysTrue, hasUpdaters ? updaters.getBody().getNodes() : null, noop,
				loopBody.getBody().getNodes());
		getScope().getCFG().getDescriptor().addControlFlowStructure(forloop);
		getScope().getControlFlowTracker().endControlFlowOf(block, hasCondition ? condition : alwaysTrue, noop,
				hasCondition ? condition : alwaysTrue, null);
		this.block = new ParsedBlock(entry, block, noop);
		return false;
	}

	private ParsedBlock visitSequentialExpressions(
			List<ASTNode> statements) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());
		ASTNode[] stmts = statements.toArray(new ASTNode[statements.size()]);
		Statement prev = null;
		Statement first = null;
		for (int i = 0; i < stmts.length; i++) {
			Expression expr = getParserContext().evaluate(stmts[i],
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			nodeList.addNode(expr);
			if (i != 0)
				nodeList.addEdge(new SequentialEdge(prev, expr));
			else
				first = expr;

			prev = expr;
		}
		return new ParsedBlock(first, nodeList, prev);
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
