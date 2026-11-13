package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.loops.ForEachLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.ForLoop;
import it.unive.jlisa.program.cfg.expression.JavaArrayAccess;
import it.unive.jlisa.program.cfg.expression.JavaComparisonLT;
import it.unive.jlisa.program.cfg.expression.JavaIncrement;
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.List;
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
				new ExpressionVisitor(getEnvironment(), getScope()));

		Expression collection = getParserContext().evaluate(node.getExpression(),
				new ExpressionVisitor(getEnvironment(), getScope()));

		// the condition of the loop is located at the whole statement, while
		// the expressions built around the collection and the loop variable
		// have no source code of their own
		SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());

		Type type = collection.getStaticType();
		if (type.isReferenceType() && type.asReferenceType().getInnerType().isArrayType()) {
			visitArray(node, item, collection, block, synth);
			return false;
		}
		Expression condition = new Equal(getScope().getCFG(), getSourceCodeLocation(node),
				new TrueLiteral(getScope().getCFG(), synth.nextLocation()),
				new HasNextForEach(getScope().getCFG(), synth.nextLocation(), collection));
		block.addNode(condition);

		JavaAssignment assignment = new JavaAssignment(getScope().getCFG(), synth.nextLocation(), item,
				new GetNextForEach(getScope().getCFG(), synth.nextLocation(), collection));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				new StatementASTVisitor(getEnvironment(), getScope()));

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

	/**
	 * Builds a loop over the elements of an array as the Java Language
	 * Specification defines it (JLS 14.14.2): the array is evaluated once, and
	 * the loop runs over its indexes,
	 * 
	 * <pre>
	 * T[] $a = expression;
	 * for (int $i = 0; $i < $a.length; $i++) {
	 * 	item = $a[$i];
	 * 	body
	 * }
	 * </pre>
	 * 
	 * where {@code $a} and {@code $i} are hidden variables, so that the loop
	 * is precise, as any loop over the cells of an array.
	 */
	private void visitArray(
			EnhancedForStatement node,
			Expression item,
			Expression collection,
			NodeList<CFG, Statement, Edge> block,
			SyntheticCodeLocationManager synth) {
		CFG cfg = getScope().getCFG();
		Type arrayType = collection.getStaticType();
		Type elementType = arrayType.asReferenceType().getInnerType().asArrayType().getInnerType();
		// unique for each loop, and not valid identifiers of the program
		String suffix = "@" + node.getStartPosition();
		String arrayName = "$foreach-array" + suffix;
		String indexName = "$foreach-index" + suffix;

		JavaAssignment initArray = new JavaAssignment(cfg, synth.nextLocation(),
				declare(arrayName, arrayType, synth), collection, true);
		JavaAssignment initIndex = new JavaAssignment(cfg, synth.nextLocation(),
				declare(indexName, JavaIntType.INSTANCE, synth), new IntLiteral(cfg, synth.nextLocation(), 0), true);
		block.addNode(initArray);
		block.addNode(initIndex);
		block.addEdge(new SequentialEdge(initArray, initIndex));

		Expression condition = new JavaComparisonLT(cfg, getSourceCodeLocation(node),
				new VariableRef(cfg, synth.nextLocation(), indexName, JavaIntType.INSTANCE),
				new JavaAccessInstanceGlobal(cfg, synth.nextLocation(),
						new VariableRef(cfg, synth.nextLocation(), arrayName, arrayType), "length"));
		block.addNode(condition);
		block.addEdge(new SequentialEdge(initIndex, condition));

		JavaAssignment assignment = new JavaAssignment(cfg, synth.nextLocation(), item,
				new JavaArrayAccess(cfg, elementType, synth.nextLocation(),
						new VariableRef(cfg, synth.nextLocation(), arrayName, arrayType),
						new VariableRef(cfg, synth.nextLocation(), indexName, JavaIntType.INSTANCE)));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				new StatementASTVisitor(getEnvironment(), getScope()));
		block.mergeWith(loopBody.getBody());
		block.addEdge(new SequentialEdge(assignment, loopBody.getBegin()));

		// the target of continue statements, even if the end of the body
		// cannot be reached
		JavaIncrement increment = new JavaIncrement(cfg, synth.nextLocation(), false,
				new VariableRef(cfg, synth.nextLocation(), indexName, JavaIntType.INSTANCE));
		block.addNode(increment);
		if (loopBody.canBeContinued())
			block.addEdge(new SequentialEdge(loopBody.getEnd(), increment));
		block.addEdge(new SequentialEdge(increment, condition));

		Statement noop = new NoOp(cfg, synth.nextLocation());
		block.addNode(noop);
		block.addEdge(new FalseEdge(condition, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		ForLoop forLoop = new ForLoop(block, List.of(initArray, initIndex), condition, List.of(increment), noop,
				loopBody.getBody().getNodes());
		cfg.getDescriptor().addControlFlowStructure(forLoop);
		getScope().getControlFlowTracker().endControlFlowOf(block, condition, noop, increment, null);
		this.block = new ParsedBlock(initArray, block, noop);
	}

	/**
	 * Declares a hidden local variable of the loop, yielding its reference.
	 */
	private VariableRef declare(
			String name,
			Type type,
			SyntheticCodeLocationManager synth) {
		VariableRef ref = new VariableRef(getScope().getCFG(), synth.nextLocation(), name, type);
		getScope().getTracker().addVariable(name, ref, ref.getAnnotations());
		getParserContext().addVariableType(getScope().getCFG(),
				new VariableInfo(name, getScope().getTracker().getLocalVariable(name)), type);
		return ref;
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
