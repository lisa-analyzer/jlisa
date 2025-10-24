package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.loops.DoWhileLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.ForEachLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.ForLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.SynchronizedBlock;
import it.unive.jlisa.program.cfg.controlflow.loops.WhileLoop;
import it.unive.jlisa.program.cfg.controlflow.switches.DefaultSwitchCase;
import it.unive.jlisa.program.cfg.controlflow.switches.Switch;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchDefault;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchEqualityCheck;
import it.unive.jlisa.program.cfg.expression.JavaCastExpression;
import it.unive.jlisa.program.cfg.expression.JavaNewArrayWithInitializer;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall;
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaThrow;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaBreak;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaContinue;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class StatementASTVisitor extends BaseCodeElementASTVisitor {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(StatementASTVisitor.class);

	private CFG cfg;

	private ParsedBlock block;

	private final ControlFlowTracker control;

	private JavaLocalVariableTracker tracker;

	public StatementASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			CFG cfg,
			ControlFlowTracker control,
			JavaLocalVariableTracker tracker,
			BaseUnitASTVisitor container) {
		super(parserContext, source, compilationUnit, container);
		this.cfg = cfg;
		this.tracker = tracker;
		this.control = control;
	}

	public StatementASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			CFG cfg,
			ControlFlowTracker control,
			Expression switchItem,
			JavaLocalVariableTracker tracker,
			BaseUnitASTVisitor container) {
		super(parserContext, source, compilationUnit, container);
		this.cfg = cfg;
		this.control = control;
		this.tracker = tracker;
		this.switchItem = switchItem;
	}

	public Statement getFirst() {
		return block != null ? block.getBegin() : null;
	}

	public Statement getLast() {
		return block != null ? block.getEnd() : null;
	}

	public ParsedBlock getBlock() {
		return block;
	}

	@Override
	public boolean visit(
			AssertStatement node) {

		org.eclipse.jdt.core.dom.Expression expr = node.getExpression();
		org.eclipse.jdt.core.dom.Expression msg = node.getMessage();

		ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		expr.accept(exprVisitor);
		Expression expression1 = exprVisitor.getExpression();

		Statement assrt = null;
		if (msg != null) {
			ExpressionVisitor messageVisitor = new ExpressionVisitor(this.parserContext, this.source,
					this.compilationUnit, this.cfg, this.tracker, container);
			msg.accept(messageVisitor);
			Expression expression2 = messageVisitor.getExpression();
			assrt = new AssertionStatement(cfg, getSourceCodeLocation(node), expression1, expression2);

		} else {
			assrt = new SimpleAssert(cfg, getSourceCodeLocation(node), expression1);
		}

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(assrt);
		this.block = new ParsedBlock(assrt, adj, assrt);

		return false;
	}

	@Override
	public boolean visit(
			Block node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());

		tracker.enterScope();

		Statement first = null, last = null;
		if (node.statements().isEmpty()) { // empty block
			NoOp emptyBlock = new NoOp(cfg, getSourceCodeLocation(node));
			nodeList.addNode(emptyBlock);
			first = emptyBlock;
			last = emptyBlock;
		} else {
			for (Object o : node.statements()) {
				StatementASTVisitor stmtVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg,
						this.control, this.tracker, container);
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
		tracker.exitScope(last);

		this.block = new ParsedBlock(first, nodeList, last);
		return false;
	}

	@Override
	public boolean visit(
			BreakStatement node) {
		JavaBreak br = new JavaBreak(cfg, getSourceCodeLocation(node));

		// TODO: labels
		control.addModifier(br);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(br);
		this.block = new ParsedBlock(br, adj, br);
		return false;
	}

	@Override
	public boolean visit(
			ConstructorInvocation node) {
		if (!node.typeArguments().isEmpty())
			throw new ParsingException("constructor-invocation",
					ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Constructor invocation statements with type arguments are not supported.",
					getSourceCodeLocation(node));

		// get the type from the descriptor
		Expression thisExpression = new VariableRef(cfg,
				parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), "this");
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);

		if (container instanceof ClassASTVisitor && ((ClassASTVisitor) container).enclosing != null) {
			Expression enclExpression = new VariableRef(cfg,
					parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), "$enclosing");
			parameters.add(enclExpression);
		}

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
						tracker, container);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}
		}
		JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, getSourceCodeLocationManager(node, true).nextColumn(),
				Call.CallType.INSTANCE, null, this.cfg.getDescriptor().getName(),
				parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);

		return false;
	}

	@Override
	public boolean visit(
			ContinueStatement node) {
		JavaContinue cnt = new JavaContinue(cfg, getSourceCodeLocation(node));

		// TODO: labels
		control.addModifier(cnt);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(cnt);
		this.block = new ParsedBlock(cnt, adj, cnt);

		return false;
	}

	@Override
	public boolean visit(
			DoStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		tracker.enterScope();

		StatementASTVisitor doBodyVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);
		if (node.getBody() == null)
			return false; // parsing error

		node.getBody().accept(doBodyVisitor);

		ParsedBlock loopBody = doBodyVisitor.getBlock();

		Statement entry;

		block.mergeWith(loopBody.getBody());
		entry = loopBody.getBegin();

		ExpressionVisitor whileCondition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		node.getExpression().accept(whileCondition);
		Expression expression = whileCondition.getExpression();
		Statement noop = new NoOp(this.cfg, expression.getLocation());

		boolean isConditionDeadcode = !loopBody.canBeContinued();

		if (!isConditionDeadcode) {
			block.addNode(expression);
			block.addEdge(new SequentialEdge(loopBody.getEnd(), expression));

			block.addEdge(new TrueEdge(expression, loopBody.getBegin()));

			block.addNode(noop);
			block.addEdge(new FalseEdge(expression, noop));

			// TODO: labels
			DoWhileLoop doWhileLoop = new DoWhileLoop(block, expression, noop, loopBody.getBody().getNodes());
			this.cfg.getDescriptor().addControlFlowStructure(doWhileLoop);
			this.control.endControlFlowOf(block, expression, noop, expression, null);
		} else {
			LOG.warn("The last statement of do-while's body stops the execution, then the guard is not reachable.");
		}

		this.block = new ParsedBlock(entry, block, isConditionDeadcode ? loopBody.getEnd() : noop);
		tracker.exitScope(noop);

		return false;
	}

	@Override
	public boolean visit(
			EmptyStatement node) {

		NoOp noop = new NoOp(cfg, getSourceCodeLocation(node));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(noop);
		this.block = new ParsedBlock(noop, adj, noop);

		return false;
	}

	@Override
	public boolean visit(
			EnhancedForStatement node) {

		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		tracker.enterScope();

		ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		node.getParameter().accept(itemVisitor);
		Expression item = itemVisitor.getExpression();

		ExpressionVisitor collectionVisitor = new ExpressionVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.tracker, container);
		node.getExpression().accept(collectionVisitor);
		Expression collection = collectionVisitor.getExpression();

		SourceCodeLocationManager locationManager = getSourceCodeLocationManager(node);
		Expression condition = new Equal(cfg, locationManager.nextColumn(),
				new TrueLiteral(cfg, locationManager.nextColumn()),
				new HasNextForEach(cfg, locationManager.nextColumn(), collection));
		block.addNode(condition);

		JavaAssignment assignment = new JavaAssignment(cfg, locationManager.nextColumn(), item,
				new GetNextForEach(cfg, locationManager.nextColumn(), collection));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		StatementASTVisitor loopBodyVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);

		node.getBody().accept(loopBodyVisitor);

		ParsedBlock loopBody = loopBodyVisitor.getBlock();

		block.mergeWith(loopBody.getBody());

		block.addEdge(new SequentialEdge(assignment, loopBody.getBegin()));

		if (loopBody.canBeContinued())
			block.addEdge(new SequentialEdge(loopBody.getEnd(), condition));
		Statement noop = new NoOp(this.cfg,
				parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
		block.addNode(noop);

		block.addEdge(new FalseEdge(condition, noop));

		tracker.exitScope(noop);

		// TODO: labels
		ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop,
				loopBody.getBody().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(forEachLoop);
		this.control.endControlFlowOf(block, condition, noop, condition, null);
		this.block = new ParsedBlock(condition, block, noop);

		return false;
	}

	@Override
	public boolean visit(
			ExpressionStatement node) {
		ExpressionVisitor expressionVisitor = new ExpressionVisitor(parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		node.getExpression().accept(expressionVisitor);
		Expression expr = expressionVisitor.getExpression();

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(expr);
		this.block = new ParsedBlock(expr, adj, expr);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(
			ForStatement node) {

		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		tracker.enterScope();

		ParsedBlock initializers = visitSequentialExpressions(node.initializers());

		boolean hasInitalizers = initializers.getBegin() != null && initializers.getEnd() != null;
		SyntheticCodeLocationManager syntheticLocationManager = parserContext
				.getCurrentSyntheticCodeLocationManager(source);
		NoOp noInit = new NoOp(cfg, syntheticLocationManager.nextLocation());
		Statement entry;
		if (hasInitalizers) {
			block.mergeWith(initializers.getBody());
			entry = initializers.getBegin();
		} else {
			block.addNode(noInit);
			entry = noInit;
		}

		ExpressionVisitor conditionExpr = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		if (node.getExpression() != null)
			node.getExpression().accept(conditionExpr);
		Expression condition = conditionExpr.getExpression();
		Statement alwaysTrue = new Equal(cfg, syntheticLocationManager.nextLocation(),
				new TrueLiteral(cfg, syntheticLocationManager.nextLocation()),
				new TrueLiteral(cfg, syntheticLocationManager.nextLocation()));

		boolean hasCondition = condition != null;

		if (hasCondition) {
			block.addNode(condition);
			block.addEdge(new SequentialEdge(hasInitalizers ? initializers.getEnd() : noInit, condition));
		} else {
			block.addNode(alwaysTrue);
			block.addEdge(new SequentialEdge(hasInitalizers ? initializers.getEnd() : noInit, alwaysTrue));
		}

		StatementASTVisitor loopBodyVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);

		node.getBody().accept(loopBodyVisitor);

		ParsedBlock loopBody = loopBodyVisitor.getBlock();

		block.mergeWith(loopBody.getBody());

		block.addEdge(new TrueEdge(hasCondition ? condition : alwaysTrue, loopBody.getBegin()));

		ParsedBlock updaters = visitSequentialExpressions(node.updaters());

		boolean hasUpdaters = updaters.getBegin() != null && updaters.getEnd() != null;

		boolean areUpdatersDeadcode = !loopBody.canBeContinued();

		if (!areUpdatersDeadcode)
			block.mergeWith(updaters.getBody());
		else if (hasUpdaters)
			LOG.warn("The last statement of for's body stops the execution, then updaters are not reachable.");

		Statement noop = new NoOp(this.cfg,
				hasCondition ? condition.getLocation() : syntheticLocationManager.nextLocation());
		block.addNode(noop);

		if (!areUpdatersDeadcode) {
			block.addEdge(new SequentialEdge(loopBody.getEnd(),
					hasUpdaters ? updaters.getBegin() : hasCondition ? condition : alwaysTrue));
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getEnd() : loopBody.getEnd(),
					hasCondition ? condition : alwaysTrue));
		}

		block.addEdge(new FalseEdge(hasCondition ? condition : alwaysTrue, noop));

		tracker.exitScope(noop);

		// TODO: labels
		ForLoop forloop = new ForLoop(block, hasInitalizers ? initializers.getBody().getNodes() : null,
				hasCondition ? condition : alwaysTrue, hasUpdaters ? updaters.getBody().getNodes() : null, noop,
				loopBody.getBody().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(forloop);
		this.control.endControlFlowOf(block, hasCondition ? condition : alwaysTrue, noop,
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
			ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
					this.cfg, this.tracker, container);
			stmts[i].accept(visitor);
			Expression expr = visitor.getExpression();
			nodeList.addNode(expr);
			if (i != 0)
				nodeList.addEdge(new SequentialEdge(prev, expr));
			else
				first = expr;

			prev = expr;
		}
		this.block = new ParsedBlock(first, nodeList, prev);
		return this.block;
	}

	@Override
	public boolean visit(
			IfStatement node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());
		ExpressionVisitor conditionVisitor = new ExpressionVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.tracker, container);

		tracker.enterScope();
		node.getExpression().accept(conditionVisitor);

		Expression condition = conditionVisitor.getExpression();
		nodeList.addNode(condition);

		StatementASTVisitor trueVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.control, this.tracker, container);

		node.getThenStatement().accept(trueVisitor);
		ParsedBlock trueBlock = trueVisitor.getBlock();

		nodeList.mergeWith(trueBlock.getBody());

		nodeList.addEdge(new TrueEdge(condition, trueVisitor.getFirst()));

		Statement noop = new NoOp(cfg, condition.getLocation());

		tracker.exitScope(noop);

		if (trueBlock.canBeContinued()) {
			nodeList.addNode(noop);
			nodeList.addEdge(new SequentialEdge(trueBlock.getEnd(), noop));
		}

		StatementASTVisitor falseVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);

		ParsedBlock falseBlock = null;

		tracker.enterScope();

		if (node.getElseStatement() != null) {
			node.getElseStatement().accept(falseVisitor);
			if (node.getElseStatement() != null) {

				falseBlock = falseVisitor.getBlock();

				nodeList.mergeWith(falseBlock.getBody());

				nodeList.addEdge(new FalseEdge(condition, falseVisitor.getFirst()));
				if (falseBlock.canBeContinued()) {
					nodeList.addNode(noop);
					nodeList.addEdge(new SequentialEdge(falseBlock.getEnd(), noop));
				}
			}
		} else {
			nodeList.addNode(noop);
			nodeList.addEdge(new FalseEdge(condition, noop));
		}

		tracker.exitScope(noop);

		Statement follower = null;
		Statement lastBlockStatement = null;

		if (trueBlock == null || (trueBlock != null && trueBlock.canBeContinued())
				|| falseBlock == null || (falseBlock != null && falseBlock.canBeContinued())) {
			nodeList.addNode(noop);
			follower = noop;
			lastBlockStatement = noop;
		}

		this.cfg.getDescriptor()
				.addControlFlowStructure(new IfThenElse(nodeList, condition, follower,
						trueBlock != null ? trueBlock.getBody().getNodes() : Collections.emptyList(),
						falseBlock != null ? falseBlock.getBody().getNodes() : Collections.emptyList()));
		this.block = new ParsedBlock(condition, nodeList, lastBlockStatement);

		return false;
	}

	@Override
	public boolean visit(
			LabeledStatement node) {
		throw new ParsingException("labeled-statement",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Labeled statements are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			ReturnStatement node) {
		ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		if (node.getExpression() != null) {
			node.getExpression().accept(visitor);
		}
		Expression e = visitor.getExpression();
		Statement ret;
		if (e == null) {
			ret = new Ret(cfg, getSourceCodeLocation(node));
		} else {
			ret = new Return(cfg, getSourceCodeLocation(node), e);
		}

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(ret);
		this.block = new ParsedBlock(ret, adj, ret);

		return false;
	}

	@Override
	public boolean visit(
			SuperConstructorInvocation node) {
		Unit unit = cfg.getDescriptor().getUnit();
		if (!(unit instanceof ClassUnit)) {
			throw new RuntimeException("The unit must be a ClassUnit when dealing with SuperConstructorInvocation");
		}

		// TODO there are cases where we should pass an enclosing instance
		// to the super constructor, but they are really rare and hard to handle
		// for now, we just live with it and we will get an open call

		ClassUnit classUnit = (ClassUnit) unit;
		String superclassName = classUnit.getImmediateAncestors().iterator().next().getName();
		String simpleName = superclassName.contains(".")
				? superclassName.substring(superclassName.lastIndexOf(".") + 1)
				: superclassName;

		Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this",
				parserContext.getVariableStaticTypeFromUnitAndGlobals(cfg, "this"));
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg,
						tracker, container);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				parameters.add(expr);
			}
		}

		JavaUnresolvedCall call = new JavaUnresolvedCall(cfg, getSourceCodeLocationManager(node, true).nextColumn(),
				Call.CallType.INSTANCE, superclassName, simpleName, parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);

		return false;
	}

	@Override
	public boolean visit(
			SwitchCase node) {
		Expression expr;
		if (switchItem.getStaticType().isReferenceType()
				&& switchItem.getStaticType().asReferenceType().getInnerType().isUnitType()
				&& switchItem.getStaticType().asReferenceType().getInnerType().asUnitType()
						.getUnit() instanceof EnumUnit) {
			// we are switching over an enum, so we check it against its fields
			if (node.expressions().size() != 1)
				throw new ParsingException("switch-case",
						ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Enum switch cases with multiple items are not supported.",
						getSourceCodeLocation(node));
			Object arg = node.expressions().iterator().next();
			if (!(arg instanceof SimpleName))
				throw new ParsingException("switch-case",
						ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Enum switch cases with non-simple names are not supported.",
						getSourceCodeLocation(node));
			SimpleName name = (SimpleName) arg;
			EnumUnit switchType = (EnumUnit) switchItem.getStaticType().asReferenceType().getInnerType().asUnitType()
					.getUnit();
			Global global = switchType.getGlobal(name.getIdentifier());
			if (global == null)
				throw new ParsingException("switch-case",
						ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Enum switch case " + name.getIdentifier() + " not found in enum " + switchType.getName(),
						getSourceCodeLocation(node));
			expr = new JavaAccessGlobal(cfg, getSourceCodeLocation(node), switchType, global);
		} else {
			ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
					this.cfg, this.tracker, container);
			node.accept(exprVisitor);
			expr = exprVisitor.getExpression();
		}

		SourceCodeLocationManager mgr = getSourceCodeLocationManager(node);
		Statement st = expr != null ? new SwitchEqualityCheck(cfg, mgr.nextColumn(), switchItem, expr)
				: new SwitchDefault(cfg, mgr.nextColumn());

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(st);
		this.block = new ParsedBlock(st, adj, st);

		return false;
	}

	private Expression switchItem = null;

	@Override
	public boolean visit(
			SwitchStatement node) {

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		node.getExpression().accept(itemVisitor);
		switchItem = itemVisitor.getExpression();
		Statement noop = new NoOp(this.cfg,
				parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
		adj.addNode(noop);

		List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases = new ArrayList<>();

		DefaultSwitchCase defaultCase = null;
		SwitchDefault switchDefault = null;

		List<SwitchEqualityCheck> workList = new ArrayList<>();

		List<Statement> caseInstrs = new ArrayList<>();
		Statement lastCaseInstr = null;

		tracker.enterScope();

		Statement first = null, last = null;
		for (Object o : node.statements()) {
			StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit,
					cfg, control, switchItem, tracker, container);
			((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);

			ParsedBlock caseBlock = statementASTVisitor.getBlock();
			boolean isEmptyBlock = caseBlock == null || caseBlock.getBody().getNodes().isEmpty();
			NoOp emptyBlock = null;

			if (isEmptyBlock) {
				emptyBlock = new NoOp(cfg,
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
				adj.addNode(emptyBlock);
			} else {
				adj.mergeWith(caseBlock.getBody());
				caseInstrs.addAll(caseBlock.getBody().getNodes());
				lastCaseInstr = caseBlock.getEnd();
			}

			if (o instanceof SwitchCase) {
				if (caseBlock.getBegin() instanceof SwitchEqualityCheck)
					workList.add((SwitchEqualityCheck) caseBlock.getBegin());
				else if (caseBlock.getBegin() instanceof SwitchDefault) {
					switchDefault = (SwitchDefault) caseBlock.getBegin();
				}
			} else if (o instanceof BreakStatement) {
				for (SwitchEqualityCheck switchCondition : workList) {

					adj.addEdge(new TrueEdge(switchCondition,
							getFirstInstructionAfterSwitchInstr(switchCondition, caseInstrs)));
					cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition,
							caseInstrs));

				}

				if (switchDefault != null) {
					defaultCase = new DefaultSwitchCase(switchDefault, caseInstrs);
					Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, caseInstrs);
					if (follower != null) {
						adj.addEdge(new SequentialEdge(switchDefault, follower));
					} else {
						emptyBlock = new NoOp(cfg,
								parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
						adj.addNode(emptyBlock);
						adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
					}
				}

				workList = new ArrayList<>();
				caseInstrs = new ArrayList<>();
			}

			if (first == null) {
				first = isEmptyBlock ? emptyBlock : caseBlock.getBegin();
			}
			if (last != null) {
				if (!(last instanceof SwitchEqualityCheck || last instanceof SwitchDefault))
					adj.addEdge(new SequentialEdge(last, caseBlock.getBegin()));
			}
			last = caseBlock.getEnd();
		}

		NoOp emptyBlock = null;

		if (switchDefault != null && defaultCase == null) {
			defaultCase = new DefaultSwitchCase(switchDefault, caseInstrs);
			Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, caseInstrs);
			if (follower != null) {
				adj.addEdge(new SequentialEdge(switchDefault, follower));
				adj.addEdge(new SequentialEdge(lastCaseInstr, noop));
			} else {
				emptyBlock = new NoOp(cfg,
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
				adj.addNode(emptyBlock);
				adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
				adj.addEdge(new SequentialEdge(emptyBlock, noop));
			}
		}

		for (SwitchEqualityCheck switchCondition : workList) {

			if (caseInstrs.size() > 1) {
				adj.addEdge(new TrueEdge(switchCondition, caseInstrs.get(1)));
			} else {
				emptyBlock = new NoOp(cfg,
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
				adj.addNode(emptyBlock);
				adj.addEdge(new TrueEdge(defaultCase.getEntry(), emptyBlock));
			}

			cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, caseInstrs));
		}

		for (int i = 0; i < cases.size() - 1; i++)
			adj.addEdge(new FalseEdge(cases.get(i).getCondition(), cases.get(i + 1).getCondition()));

		if (defaultCase != null && !cases.isEmpty())
			adj.addEdge(new FalseEdge(cases.getLast().getCondition(), defaultCase.getEntry()));

		lazySwitchEdgeBindingCleanUp(adj, cases, defaultCase != null ? defaultCase.getEntry() : null);

		if (node.statements().isEmpty() || (cases.isEmpty() && defaultCase == null)) {
			emptyBlock = new NoOp(cfg,
					parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
			adj.addNode(emptyBlock);
			adj.addEdge(new SequentialEdge(emptyBlock, noop));
			first = emptyBlock;
		}

		tracker.exitScope(noop);

		// TODO: labels
		this.cfg.getDescriptor()
				.addControlFlowStructure(new Switch(adj,
						!cases.isEmpty() ? cases.getFirst().getCondition()
								: defaultCase != null ? defaultCase.getEntry() : emptyBlock,
						noop,
						cases.toArray(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase[cases.size()]),
						defaultCase));
		this.control.endControlFlowOf(adj, !cases.isEmpty() ? cases.getFirst().getCondition()
				: defaultCase != null ? defaultCase.getEntry() : emptyBlock, noop, null, null);
		this.block = new ParsedBlock(first, adj, noop);
		return false;
	}

	private void lazySwitchEdgeBindingCleanUp(
			NodeList<CFG, Statement, Edge> adj,
			List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases,
			SwitchDefault switchDefault) {
		for (it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase c : cases) {
			for (Edge e : adj.getEdges()) {
				if (e.getDestination().equals(c.getCondition())
						&& !(e instanceof FalseEdge && (e.getSource() instanceof SwitchEqualityCheck
								|| e.getSource() instanceof SwitchDefault))) {
					adj.addEdge(
							e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(adj, c.getCondition())));
					adj.removeEdge(e);
				}
			}
		}

		if (switchDefault != null) {
			for (Edge e : adj.getEdges()) {
				if (e.getDestination().equals(switchDefault)
						&& !(e instanceof FalseEdge && e.getSource() instanceof SwitchEqualityCheck)) {
					adj.addEdge(e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(adj, switchDefault)));
					adj.removeEdge(e);
				}
			}
		}
	}

	private Statement getFirstInstructionAfterSwitchInstr(
			NodeList<CFG, Statement, Edge> adj,
			Statement stmt) {
		return getFirstInstructionAfterSwitchInstrRecursive(adj, stmt, new HashSet<>());
	}

	private Statement getFirstInstructionAfterSwitchInstrRecursive(
			NodeList<CFG, Statement, Edge> block,
			Statement stmt,
			Set<Statement> seen) {

		if (!seen.contains(stmt)) {
			seen.add(stmt);

			List<TrueEdge> trueSwitchCaseEdges = new ArrayList<>();
			List<FalseEdge> falseSwitchCaseEdges = new ArrayList<>();
			List<SequentialEdge> defaultEdges = new ArrayList<>();

			for (Edge e : block.getEdges()) {
				if (e.getSource().equals(stmt)) {
					if (!(e.getDestination() instanceof SwitchEqualityCheck
							|| e.getDestination() instanceof SwitchDefault)) {
						return e.getDestination();
					} else if (stmt instanceof SwitchEqualityCheck) {
						if (e instanceof TrueEdge)
							trueSwitchCaseEdges.add((TrueEdge) e);
						else if (e instanceof FalseEdge)
							falseSwitchCaseEdges.add((FalseEdge) e);
					} else if (stmt instanceof SwitchDefault) {
						defaultEdges.add((SequentialEdge) e);
					}
				}
			}

			for (TrueEdge e : trueSwitchCaseEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if (res != null)
					return res;
			}

			for (FalseEdge e : falseSwitchCaseEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if (res != null)
					return res;
			}

			for (SequentialEdge e : defaultEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if (res != null)
					return res;
			}
		}
		return null;
	}

	private Statement getFirstInstructionAfterSwitchInstr(
			Statement stmt,
			List<Statement> instrList) {
		Iterator<Statement> iter = instrList.iterator();

		boolean found = false;
		while (iter.hasNext() && !found) {
			if (iter.next().equals(stmt))
				found = true;
		}

		if (found) {
			while (iter.hasNext()) {
				Statement next = iter.next();
				if (!(next instanceof SwitchDefault || next instanceof SwitchEqualityCheck))
					return next;
			}
		}

		return null;
	}

	@Override
	public boolean visit(
			SynchronizedStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		ExpressionVisitor synchTargetVisitor = new ExpressionVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.tracker, container);
		node.getExpression().accept(synchTargetVisitor);
		Expression syncTarget = synchTargetVisitor.getExpression();

		SyntheticCodeLocationManager syntheticLocMan = parserContext.getCurrentSyntheticCodeLocationManager(source);
		Statement syntheticCondition = new NotEqual(cfg, syntheticLocMan.nextLocation(), syncTarget,
				new JavaNullLiteral(cfg, syntheticLocMan.nextLocation()));

		adj.addNode(syntheticCondition);

		StatementASTVisitor bodyVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.control, this.tracker, container);
		node.getBody().accept(bodyVisitor);

		ParsedBlock synchronizedBody = bodyVisitor.getBlock();

		adj.mergeWith(synchronizedBody.getBody());

		adj.addEdge(new TrueEdge(syntheticCondition, synchronizedBody.getBegin()));

		Statement noop = new NoOp(cfg, syntheticLocMan.nextLocation());

		if (synchronizedBody.canBeContinued()) {
			adj.addNode(noop);
			adj.addEdge(new SequentialEdge(synchronizedBody.getEnd(), noop));
		}

		JavaClassType npeType = JavaClassType.lookup("java.lang.NullPointerException");
		Statement nullPointerTrigger = new JavaThrow(cfg, syntheticLocMan.nextLocation(),
				new JavaNewObj(cfg, syntheticLocMan.nextLocation(),
						new JavaReferenceType(npeType),
						new JavaStringLiteral(cfg,
								syntheticLocMan.nextLocation(),
								"Cannot enter synchronized block because " + syncTarget + " is null")));
		adj.addNode(nullPointerTrigger);
		adj.addEdge(new FalseEdge(syntheticCondition, nullPointerTrigger));

		Statement follower = null;
		Statement lastBlockStatement = null;

		if (synchronizedBody == null || (synchronizedBody != null && synchronizedBody.canBeContinued())) {
			adj.addNode(noop);
			follower = noop;
			lastBlockStatement = noop;
		}

		this.cfg.getDescriptor().addControlFlowStructure(new SynchronizedBlock(adj, syncTarget, syntheticCondition,
				synchronizedBody.getBody().getNodes(), follower));
		this.block = new ParsedBlock(syntheticCondition, adj, lastBlockStatement);

		return false;
	}

	@Override
	public boolean visit(
			TypeDeclarationStatement node) {
		throw new ParsingException("type-declaration-statement",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Type declaration statements are not supported.",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			VariableDeclarationStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		Statement first = null, last = null;
		TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getType().accept(visitor);

		Type type = visitor.getType();
		if (type.isInMemoryType()) {
			type = new JavaReferenceType(type);
		}
		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			type = visitor.liftToArray(type, fragment);

			VariableRef ref = new VariableRef(cfg,
					getSourceCodeLocation(fragment),
					variableName, type);
			Expression initializer;

			JavaAssignment assignment;

			if (fragment.getInitializer() == null) {
				initializer = type.defaultValue(cfg,
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
				assignment = new JavaAssignment(cfg,
						parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), ref, initializer);
			} else {
				SourceCodeLocationManager loc = getSourceCodeLocationManager(fragment.getName(), true);
				org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
				ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg,
						tracker, container);
				expr.accept(exprVisitor);
				initializer = exprVisitor.getExpression();
				if (initializer == null) {
					initializer = new NullLiteral(cfg,
							parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation());
					assignment = new JavaAssignment(cfg,
							parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), ref,
							initializer);
				} else if (initializer instanceof JavaNewArrayWithInitializer) {
					assignment = new JavaAssignment(cfg, loc.getCurrentLocation(), ref,
							((JavaNewArrayWithInitializer) initializer).withStaticType(type));
				} else if (initializer.getStaticType().canBeAssignedTo(type)
						&& !type.equals(initializer.getStaticType())) {
					// implicit cast
					JavaCastExpression cast = new JavaCastExpression(cfg,
							parserContext.getCurrentSyntheticCodeLocationManager(source).nextLocation(), initializer,
							type);
					assignment = new JavaAssignment(cfg, loc.getCurrentLocation(), ref, cast);
				} else
					assignment = new JavaAssignment(cfg, loc.getCurrentLocation(), ref, initializer);
			}

			adj.addNode(assignment);
			if (getFirst() == null) {
				first = assignment;
			} else {
				adj.addEdge(new SequentialEdge(last, assignment));
			}

			if (tracker.hasVariable(variableName))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Variable " + variableName + " already exists in the cfg", getSourceCodeLocation(node));

			tracker.addVariable(variableName, assignment, ref.getAnnotations());
			parserContext.addVariableType(cfg,
					new VariableInfo(variableName, tracker.getLocalVariable(variableName)), type);

			last = assignment;
		}

		this.block = new ParsedBlock(first, adj, last);
		return false;
	}

	@Override
	public boolean visit(
			WhileStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		tracker.enterScope();

		ExpressionVisitor condition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.tracker, container);
		node.getExpression().accept(condition);
		Expression expression = condition.getExpression();

		adj.addNode(expression);

		StatementASTVisitor loopBodyVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);
		node.getBody().accept(loopBodyVisitor);

		ParsedBlock loopBody = loopBodyVisitor.getBlock();

		adj.mergeWith(loopBody.getBody());

		adj.addEdge(new TrueEdge(expression, loopBody.getBegin()));
		if (loopBody.canBeContinued())
			adj.addEdge(new SequentialEdge(loopBody.getEnd(), expression));

		Statement noop = new NoOp(this.cfg, expression.getLocation());
		adj.addNode(noop);
		adj.addEdge(new FalseEdge(expression, noop));

		tracker.exitScope(noop);

		// TODO: labels
		WhileLoop whileLoop = new WhileLoop(adj, expression, noop, loopBody.getBody().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(whileLoop);
		this.control.endControlFlowOf(adj, expression, noop, expression, null);
		this.block = new ParsedBlock(expression, adj, noop);

		return false;
	}

	@Override
	public boolean visit(
			ThrowStatement node) {
		ExpressionVisitor exprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg, tracker,
				container);
		node.getExpression().accept(exprVisitor);
		Expression expr = exprVisitor.getExpression();
		Throw th = new JavaThrow(cfg, getSourceCodeLocation(node), expr);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(th);
		this.block = new ParsedBlock(th, adj, th);
		return false;
	}

	@Override
	public boolean visit(
			TryStatement node) {
		NodeList<CFG, Statement, Edge> trycatch = new NodeList<>(new SequentialEdge());

		// normal exit points of the try-catch in case there is no finally
		// block:
		// in this case, we have to add a noop at the end of the whole try-catch
		// to use it as unique exit point in the returned triple
		Collection<Statement> normalExits = new LinkedList<>();

		// we parse the body of the try block normally
		StatementASTVisitor blockVisitor = new StatementASTVisitor(this.parserContext, this.source,
				this.compilationUnit, this.cfg, this.control, this.tracker, container);
		node.getBody().accept(blockVisitor);
		// body of the try
		ParsedBlock body = blockVisitor.getBlock();
		trycatch.mergeWith(body.getBody());
		ProtectedBlock tryBlock = new ProtectedBlock(body.getBegin(), body.getEnd(), body.getBody().getNodes());

		// we add the the end of the try block *only* if it does not end with a
		// return/throw (as it would be deadcode)
		if (body.canBeContinued())
			normalExits.add(body.getEnd());

		// we then parse each catch block, and we connect *every* instruction
		// that can end the try block to the beginning of each catch block
		List<CatchBlock> catches = new LinkedList<>();
		List<ParsedBlock> catchBodies = new LinkedList<>();
		for (int i = 0; i < node.catchClauses().size(); i++) {
			CatchClause cl = (CatchClause) node.catchClauses().get(i);
			StatementASTVisitor clVisitor = new StatementASTVisitor(this.parserContext, this.source,
					this.compilationUnit, this.cfg, this.control, this.tracker, container);
			cl.accept(clVisitor);

			CatchBlock block = clVisitor.clBlock;
			ParsedBlock visit = clVisitor.getBlock();
			catches.add(block);
			catchBodies.add(visit);
			trycatch.mergeWith(visit.getBody());

			if (body.canBeContinued())
				trycatch.addEdge(
						new ErrorEdge(body.getEnd(), visit.getBegin(), block.getIdentifier(), tryBlock,
								block.getExceptions()));
			for (Statement st : body.getBody().getNodes())
				if (st.stopsExecution())
					trycatch.addEdge(new ErrorEdge(st, visit.getBegin(), block.getIdentifier(), tryBlock,
							block.getExceptions()));

			if (visit.canBeContinued())
				// non-stopping last statement
				normalExits.add(visit.getEnd());
		}

		// lastly, we parse the finally block and
		// we connect it with the body (or the else block if it exists) and with
		// each catch block
		ParsedBlock finallyBlock = null;
		if (node.getFinally() != null) {
			StatementASTVisitor finallyVisitor = new StatementASTVisitor(this.parserContext, this.source,
					this.compilationUnit, this.cfg, this.control, this.tracker, container);
			node.getFinally().accept(finallyVisitor);
			finallyBlock = finallyVisitor.getBlock();
			trycatch.mergeWith(finallyBlock.getBody());
		}

		// this is the noop closing the whole try-catch, only if there is at
		// least one path that does
		// not return/throw anything
		Statement noop = new NoOp(cfg, getSourceCodeLocation(node));
		boolean usedNoop = !normalExits.isEmpty();
		if (usedNoop) {
			trycatch.addNode(noop);
			if (node.getFinally() == null)
				// if there is no finally block, we connect the noop to the
				// end of all non-terminating inner blocks. otherwise,
				// the CFGTweaker will add the edges to the finally block
				// and back to the noop
				for (Statement st : normalExits)
					if (!st.stopsExecution())
						trycatch.addEdge(new SequentialEdge(st, noop));
		}

		// build protection block
		this.cfg.getDescriptor().addProtectionBlock(
				new ProtectionBlock(
						tryBlock,
						catches,
						null,
						finallyBlock == null ? null
								: new ProtectedBlock(
										finallyBlock.getBegin(),
										finallyBlock.getEnd(),
										finallyBlock.getBody().getNodes()),
						usedNoop ? noop : null));

		this.block = new ParsedBlock(body.getBegin(), trycatch, usedNoop ? noop : null);
		return false;
	}

	private CatchBlock clBlock;

	@Override
	public boolean visit(
			CatchClause node) {
		tracker.enterScope();

		// type of the exception
		TypeASTVisitor typeVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit, container);
		node.getException().getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		type = type.isInMemoryType() ? new JavaReferenceType(type) : type;

		// exception variable
		SingleVariableDeclaration exc = node.getException();
		VariableRef v = new VariableRef(cfg, getSourceCodeLocation(exc), exc.getName().getIdentifier(), type);
		tracker.addVariable(v.toString(), v, new Annotations());
		parserContext.addVariableType(cfg,
				new VariableInfo(v.toString(), tracker != null ? tracker.getLocalVariable(v.toString()) : null),
				type);

		// exception body
		StatementASTVisitor catchBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit,
				this.cfg, this.control, this.tracker, container);
		node.getBody().accept(catchBody);
		// body of the catch clause
		ParsedBlock body = catchBody.getBlock();

		CatchBlock catchBlock = new CatchBlock(
				v,
				new ProtectedBlock(body.getBegin(), body.getEnd(), body.getBody().getNodes()),
				type);

		tracker.exitScope(body.getEnd());
		this.clBlock = catchBlock;
		this.block = body;
		return false;
	}

}