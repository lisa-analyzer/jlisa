package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.loops.ForEachLoop;
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
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
import org.eclipse.jdt.core.dom.EnhancedForStatement;

class EnhancedForStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private ParsedBlock block;

	EnhancedForStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			EnhancedForStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		getScope().getTracker().enterScope();

		Expression item = getParserContext().evaluate(node.getParameter(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		Expression collection = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		SourceCodeLocationManager locationManager = getSourceCodeLocationManager(node);
		Expression condition = new Equal(getScope().getCFG(), locationManager.nextColumn(),
				new TrueLiteral(getScope().getCFG(), locationManager.nextColumn()),
				new HasNextForEach(getScope().getCFG(), locationManager.nextColumn(), collection));
		block.addNode(condition);

		JavaAssignment assignment = new JavaAssignment(getScope().getCFG(), locationManager.nextColumn(), item,
				new GetNextForEach(getScope().getCFG(), locationManager.nextColumn(), collection));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		block.mergeWith(loopBody.getBody());

		block.addEdge(new SequentialEdge(assignment, loopBody.getBegin()));

		if (loopBody.canBeContinued())
			block.addEdge(new SequentialEdge(loopBody.getEnd(), condition));
		Statement noop = new NoOp(getScope().getCFG(),
				getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
		block.addNode(noop);

		block.addEdge(new FalseEdge(condition, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop,
				loopBody.getBody().getNodes());
		getScope().getCFG().getDescriptor().addControlFlowStructure(forEachLoop);
		getScope().getControlFlowTracker().endControlFlowOf(block, condition, noop, condition, null);
		this.block = new ParsedBlock(condition, block, noop);

		return false;
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
