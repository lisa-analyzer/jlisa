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
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAddition;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.program.annotations.Annotations;
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
import java.util.ArrayList;
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

		Expression collection = getParserContext().evaluate(node.getExpression(),
				new ExpressionVisitor(getEnvironment(), getScope()));

		Type collectionType = collection.getStaticType();
		if (collectionType.asReferenceType() != null
				&& collectionType.asReferenceType().getInnerType() instanceof JavaArrayType arrayType)
			visitArray(node, collection, arrayType);
		else
			visitIterable(node, collection);
		return false;
	}

	/**
	 * desugared to:
	 * ```
	 * T[] $arr = collection;
	 * int $len = $arr.length;
	 * for (int $i = 0; $i < $len; $i++) {
	 *     T item = $arr[$i];
	 *     body
	 * }
	 * ```
	 *
	 * includes loop unrolling
	 *
	 */
	private void visitArray(
			EnhancedForStatement node,
			Expression collection,
			JavaArrayType arrayType) {
		CFG cfg = getScope().getCFG();
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());
		SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());

		getScope().getTracker().enterScope();

		Expression item = getParserContext().evaluate(node.getParameter(),
				new ExpressionVisitor(getEnvironment(), getScope()));

		Statement noop = new NoOp(getScope().getCFG(), item.getLocation());
		block.addNode(noop);

		String id = String.valueOf(node.getStartPosition());
		String arrName = "$arr" + id;
		String lenName = "$len" + id;
		String idxName = "$i" + id;
		Type collectionType = collection.getStaticType();
		Type elementType = arrayType.getInnerType();

		VariableRef arrDecl = declare(arrName, collectionType, synth);
		VariableRef lenDecl = declare(lenName, JavaIntType.INSTANCE, synth);
		VariableRef idxDecl = declare(idxName, JavaIntType.INSTANCE, synth);

		JavaAssignment initArr = new JavaAssignment(cfg, synth.nextLocation(), arrDecl, collection);
		JavaAssignment initLen = new JavaAssignment(cfg, synth.nextLocation(), lenDecl,
				new JavaAccessInstanceGlobal(cfg, synth.nextLocation(), ref(arrName, collectionType, synth),
						"length"));
		JavaAssignment initIdx = new JavaAssignment(cfg, synth.nextLocation(), idxDecl,
				new IntLiteral(cfg, synth.nextLocation(), 0));

		block.addNode(initArr);
		block.addNode(initLen);
		block.addNode(initIdx);

		 // TODO: add a check to only unroll in simple cases:
		 // no break/continue, no nested loops, no lambdas etc...
		boolean canUnroll = true;
		int UNROLL_FACTOR = 16;

		Statement first = null;
		Statement last = null;

		if (canUnroll) {
			for (int i = 0; i < UNROLL_FACTOR; i++) {

				ParsingEnvironment env = getEnvironment().withUnrollingCopy(i + 2);

				// Expression condition = new LessThan(cfg, synth.nextLocation(),
				// 		ref(idxName, JavaIntType.INSTANCE, synth),
				// 		ref(lenName, JavaIntType.INSTANCE, synth));
				Expression condition = new Equal(cfg, synth.nextLocation(),
						new TrueLiteral(getScope().getCFG(), synth.nextLocation()),
						new HasNextForEach(getScope().getCFG(), synth.nextLocation(), collection, idxName, lenName));

				ParsedBlock loopBody = getParserContext().evaluate(
						node.getBody(),
						new StatementASTVisitor(env, getScope()));

				// item = $arr[$i]
				// JavaAssignment assignItem = new JavaAssignment(cfg, synth.nextLocation(), item,
				// 		new JavaArrayAccess(cfg, elementType, synth.nextLocation(),
				// 				ref(arrName, collectionType, synth),
				// 				ref(idxName, JavaIntType.INSTANCE, synth)));

				JavaAssignment assignItem = new JavaAssignment(cfg, synth.nextLocation(), item, new GetNextForEach(getScope().getCFG(), synth.nextLocation(), collection, idxName, lenName));

				// $i = $i + 1
				JavaAssignment increment = new JavaAssignment(cfg, synth.nextLocation(),
						ref(idxName, JavaIntType.INSTANCE, synth),
						new JavaAddition(cfg, synth.nextLocation(),
								ref(idxName, JavaIntType.INSTANCE, synth),
								new IntLiteral(cfg, synth.nextLocation(), 1)));

				block.addNode(condition);
				block.addNode(assignItem);
				block.addNode(increment);
				block.mergeWith(loopBody.getBody());
				block.addEdge(new TrueEdge(condition, assignItem));
				block.addEdge(new SequentialEdge(assignItem, loopBody.getBegin()));
				block.addEdge(new SequentialEdge(loopBody.getEnd(), increment));
				block.addEdge(new FalseEdge(condition, noop));

				if (first == null)
					first = condition;
				if (last != null)
					block.addEdge(new SequentialEdge(last, condition));

				last = increment;
			}
		}

		block.addEdge(new SequentialEdge(initArr, initLen));
		block.addEdge(new SequentialEdge(initLen, initIdx));
		block.addEdge(new SequentialEdge(initIdx, first));

		// the original loop will have copy id = 1. This s used by the assert checker to
		// figure out which one is the real assert
		ParsingEnvironment env = getEnvironment().withUnrollingCopy(1);

		// build the actual loop
		// Expression condition = new LessThan(cfg, getSourceCodeLocation(node),
		// 		ref(idxName, JavaIntType.INSTANCE, synth),
		// 		ref(lenName, JavaIntType.INSTANCE, synth));
		Expression condition = new Equal(cfg, synth.nextLocation(),
				new TrueLiteral(getScope().getCFG(), synth.nextLocation()),
				new HasNextForEach(getScope().getCFG(), synth.nextLocation(), collection, idxName, lenName));


		// item = $arr[$i]
		// JavaAssignment assignItem = new JavaAssignment(cfg, synth.nextLocation(), item,
		// 		new JavaArrayAccess(cfg, elementType, synth.nextLocation(),
		// 				ref(arrName, collectionType, synth),
		// 				ref(idxName, JavaIntType.INSTANCE, synth)));
		JavaAssignment assignItem = new JavaAssignment(cfg, synth.nextLocation(), item, new GetNextForEach(getScope().getCFG(), synth.nextLocation(), collection, idxName, lenName));


		// $i = $i + 1
		JavaAssignment increment = new JavaAssignment(cfg, synth.nextLocation(),
				ref(idxName, JavaIntType.INSTANCE, synth),
				new JavaAddition(cfg, synth.nextLocation(),
						ref(idxName, JavaIntType.INSTANCE, synth),
						new IntLiteral(cfg, synth.nextLocation(), 1)));

		block.addNode(condition);
		block.addNode(assignItem);
		block.addNode(increment);
		block.addEdge(new SequentialEdge(last, condition));
		block.addEdge(new TrueEdge(condition, assignItem));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				new StatementASTVisitor(env, getScope()));
		block.mergeWith(loopBody.getBody());
		block.addEdge(new SequentialEdge(assignItem, loopBody.getBegin()));

		if (loopBody.canBeContinued())
			block.addEdge(new SequentialEdge(loopBody.getEnd(), increment));
		block.addEdge(new SequentialEdge(increment, condition));

		// Statement noop = new NoOp(cfg, synth.nextLocation());
		block.addNode(noop);
		block.addEdge(new FalseEdge(condition, noop));

		getScope().getTracker().exitScope(noop);

		List<Statement> bodyNodes = new ArrayList<>(loopBody.getBody().getNodes());
		bodyNodes.add(assignItem);

		ForLoop forLoop = new ForLoop(block, List.of(initArr, initLen, initIdx), condition, List.of(increment),
				noop, bodyNodes);
		cfg.getDescriptor().addControlFlowStructure(forLoop);

		// a `continue` goes to the increment
		getScope().getControlFlowTracker().endControlFlowOf(block, condition, noop, increment, null);

		this.block = new ParsedBlock(initArr, block, noop);
	}

	/**
	 * Non-array collections: unchanged from the previous implementation
	 * (hasNext/getNext evaluate to top)
	 */
	private void visitIterable(
			EnhancedForStatement node,
			Expression collection) {
		CFG cfg = getScope().getCFG();
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());
		SyntheticCodeLocationManager synth = getParserContext().getCurrentSyntheticCodeLocationManager(getSource());

		getScope().getTracker().enterScope();

		Expression item = getParserContext().evaluate(node.getParameter(),
				new ExpressionVisitor(getEnvironment(), getScope()));

		Expression condition = new Equal(cfg, getSourceCodeLocation(node),
				new TrueLiteral(cfg, synth.nextLocation()),
				new HasNextForEach(cfg, synth.nextLocation(), collection, "$i", "$length"));
		block.addNode(condition);

		JavaAssignment assignment = new JavaAssignment(cfg, synth.nextLocation(), item,
				new GetNextForEach(cfg, synth.nextLocation(), collection, "$i", "$length"));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		ParsedBlock loopBody = getParserContext().evaluate(
				node.getBody(),
				new StatementASTVisitor(getEnvironment(), getScope()));
		block.mergeWith(loopBody.getBody());
		block.addEdge(new SequentialEdge(assignment, loopBody.getBegin()));
		if (loopBody.canBeContinued())
			block.addEdge(new SequentialEdge(loopBody.getEnd(), condition));

		Statement noop = new NoOp(cfg, synth.nextLocation());
		block.addNode(noop);
		block.addEdge(new FalseEdge(condition, noop));

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop,
				loopBody.getBody().getNodes());
		cfg.getDescriptor().addControlFlowStructure(forEachLoop);
		getScope().getControlFlowTracker().endControlFlowOf(block, condition, noop, condition, null);
		this.block = new ParsedBlock(condition, block, noop);
	}

	private VariableRef declare(
			String name,
			Type type,
			SyntheticCodeLocationManager synth) {
		VariableRef v = ref(name, type, synth);
		getScope().getTracker().addVariable(name, v, new Annotations());
		getParserContext().addVariableType(getScope().getCFG(),
				new VariableInfo(name,
						getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(name) : null),
				type);
		return v;
	}

	private VariableRef ref(
			String name,
			Type type,
			SyntheticCodeLocationManager synth) {
		return new VariableRef(getScope().getCFG(), synth.nextLocation(), name, type);
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
