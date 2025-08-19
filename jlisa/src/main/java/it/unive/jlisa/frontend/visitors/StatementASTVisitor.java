package it.unive.jlisa.frontend.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.controlflow.loops.DoWhileLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.ForEachLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.ForLoop;
import it.unive.jlisa.program.cfg.controlflow.loops.WhileLoop;
import it.unive.jlisa.program.cfg.controlflow.switches.DefaultSwitchCase;
import it.unive.jlisa.program.cfg.controlflow.switches.Switch;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchDefault;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchEqualityCheck;
import it.unive.jlisa.program.cfg.expression.instrumentations.EmptyBody;
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaBreak;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaContinue;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
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
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import it.unive.lisa.util.frontend.LocalVariableTracker;
import it.unive.lisa.util.frontend.ParsedBlock;

public class StatementASTVisitor extends JavaASTVisitor {

	private CFG cfg;

	private ParsedBlock block;

	private final LocalVariableTracker tracker;

	private final ControlFlowTracker control;

	public StatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg, ControlFlowTracker control) {
		super(parserContext, source, compilationUnit);
		this.cfg = cfg;
		tracker = new LocalVariableTracker(cfg, cfg.getDescriptor());
		this.control = control;   
	}

	public StatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg, ControlFlowTracker control, Expression switchItem) {
		super(parserContext, source, compilationUnit);
		this.cfg = cfg;
		tracker = new LocalVariableTracker(cfg, cfg.getDescriptor());
		this.control = control;
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
	public boolean visit(AssertStatement node) {

		org.eclipse.jdt.core.dom.Expression expr = node.getExpression();
		org.eclipse.jdt.core.dom.Expression msg = node.getMessage();


		ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		expr.accept(exprVisitor);
		Expression expression1 = exprVisitor.getExpression(); 


		Statement assrt = null;
		if(msg != null) {
			ExpressionVisitor messageVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
			msg.accept(messageVisitor);
			Expression expression2 = messageVisitor.getExpression(); 
			assrt = new AssertionStatement(cfg, getSourceCodeLocation(node), expression1,expression2);

		} else {
			assrt = new SimpleAssert(cfg, getSourceCodeLocation(node), expression1);
		}

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(assrt);
		this.block = new ParsedBlock(assrt, adj, assrt);

		return false;
	}

	@Override
	public boolean visit(Block node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());

		Statement first = null, last = null;
		for (Object o : node.statements()) {
			StatementASTVisitor stmtVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg, this.control);
			((org.eclipse.jdt.core.dom.Statement) o).accept(stmtVisitor);

			boolean isEmptyBlock = stmtVisitor.getBlock().getBody().getNodes().isEmpty();
			EmptyBody emptyBlock = null;
			if (isEmptyBlock) {
				emptyBlock = new EmptyBody(cfg, getSourceCodeLocation(node));
				nodeList.addNode(emptyBlock);
			} else {
				nodeList.mergeWith(stmtVisitor.getBlock().getBody());
			}

			if (first == null) {
				first = isEmptyBlock ? emptyBlock : stmtVisitor.getBlock().getBegin();
			}

			if (last != null) {
				nodeList.addEdge(new SequentialEdge(last, stmtVisitor.getBlock().getBegin()));
			}
			last = stmtVisitor.getBlock().getEnd();
		}

		this.block = new ParsedBlock(first, nodeList, last);
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		JavaBreak br = new JavaBreak(cfg, getSourceCodeLocation(node));

		//TODO: labels
		control.addModifier(br);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(br);
		this.block = new ParsedBlock(br, adj, br);
		return false;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (!node.typeArguments().isEmpty()) {
			parserContext.addException(
					new ParsingException("constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
							"Constructor invocation statements with type arguments are not supported.",
							getSourceCodeLocation(node))
					);
		}


		// get the type from the descriptor
		Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this");
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e  = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				if (expr != null) {
					// This parsing error should be logged in ExpressionVisitor.
					parameters.add(expr);
				}
			}
		}
		UnresolvedCall call = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null,this.cfg.getDescriptor().getName(), parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);

		return false;
	}

	@Override
	public boolean visit(ContinueStatement node) {
		JavaContinue cnt = new JavaContinue(cfg, getSourceCodeLocation(node));

		// TODO: labels
		control.addModifier(cnt);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(cnt);
		this.block = new ParsedBlock(cnt, adj, cnt);

		return false;
	}

	@Override
	public boolean visit(DoStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		StatementASTVisitor doBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		if(node.getBody() == null)
			return false; // parsing error

		node.getBody().accept(doBody);
		Statement noBody = new EmptyBody(this.cfg, getSourceCodeLocation(node));

		Statement entry;
		boolean hasBody = doBody.getFirst() != null && doBody.getLast() != null;
		if(hasBody) {
			block.mergeWith(doBody.getBlock().getBody());
			entry = doBody.getFirst();
		} else {
			block.addNode(noBody);
			entry = noBody;
		}


		ExpressionVisitor whileCondition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(whileCondition);
		Expression expression = whileCondition.getExpression();

		if (expression == null) {
			// Parsing error. Skipping...
			return false;
		}

		block.addNode(expression);
		block.addEdge(new SequentialEdge(hasBody ? doBody.getLast() : noBody, expression));
		block.addEdge(new TrueEdge(expression, hasBody ? doBody.getFirst() : noBody));

		Statement noop = new NoOp(this.cfg, expression.getLocation());
		block.addNode(noop);
		block.addEdge(new FalseEdge(expression, noop));


		// TODO: labels
		DoWhileLoop doWhileLoop = new DoWhileLoop(block, expression, noop, doBody.getBlock().getBody().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(doWhileLoop);
		this.control.endControlFlowOf(block, expression, noop, expression, null);
		this.block = new ParsedBlock(entry, block, noop);

		return false;
	}

	@Override
	public boolean visit(EmptyStatement node) {

		return false;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {

		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getParameter().accept(itemVisitor);
		Expression item = itemVisitor.getExpression();

		ExpressionVisitor collectionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(collectionVisitor);
		Expression collection = collectionVisitor.getExpression();

		Expression condition = new Equal(cfg, item.getLocation(), new TrueLiteral(cfg, item.getLocation()), new HasNextForEach(cfg,item.getLocation(),collection));
		block.addNode(condition);

		JavaAssignment assignment = new JavaAssignment(cfg, item.getLocation(), item, new GetNextForEach(cfg,item.getLocation(),collection));
		block.addNode(assignment);
		block.addEdge(new TrueEdge(condition, assignment));

		StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		if(node.getBody() == null)
			return false; // parsing error

		node.getBody().accept(loopBody);
		Statement noBody = new EmptyBody(this.cfg, condition.getLocation());

		boolean hasBody = loopBody.getFirst() != null && loopBody.getLast() != null;
		if(hasBody)
			block.mergeWith(loopBody.getBlock().getBody());
		else
			block.addNode(noBody);

		block.addEdge( new SequentialEdge(assignment, hasBody ? loopBody.getFirst() : noBody));

		block.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, condition));

		Statement noop = new NoOp(this.cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
		block.addNode(noop);

		block.addEdge(new FalseEdge(condition, noop));

		// TODO: labels
		ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop, hasBody ? loopBody.getBlock().getBody().getNodes() : Collections.emptySet());
		this.cfg.getDescriptor().addControlFlowStructure(forEachLoop);
		this.control.endControlFlowOf(block, condition, noop, condition, null);
		this.block = new ParsedBlock(condition, block, noop);

		return false;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		ExpressionVisitor expressionVisitor = new ExpressionVisitor(parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(expressionVisitor);
		Expression expr = expressionVisitor.getExpression();

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(expr);
		this.block = new ParsedBlock(expr, adj, expr);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(ForStatement node) {

		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		ParsedBlock initializers = visitSequentialExpressions(node.initializers());

		boolean hasInitalizers = initializers.getBegin() != null && initializers.getEnd() != null;
		NoOp noInit = new NoOp(cfg, getSourceCodeLocation(node));
		Statement entry;
		if(hasInitalizers) {
			block.mergeWith(initializers.getBody());
			entry = initializers.getBegin();
		} else {
			block.addNode(noInit);
			entry = noInit;
		}

		ExpressionVisitor conditionExpr = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		if(node.getExpression() != null)
			node.getExpression().accept(conditionExpr);
		Expression condition = conditionExpr.getExpression();
		Statement alwaysTrue = new Equal(cfg, getSourceCodeLocation(node), new TrueLiteral(cfg, getSourceCodeLocation(node)), new TrueLiteral(cfg, getSourceCodeLocation(node)));

		boolean hasCondition = condition != null;

		if(hasCondition) {
			block.addNode(condition);
			block.addEdge(new SequentialEdge(hasInitalizers? initializers.getEnd() : noInit, condition));
		} else {
			block.addNode(alwaysTrue);
			block.addEdge(new SequentialEdge(hasInitalizers? initializers.getEnd() : noInit, alwaysTrue));
		}

		StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		if(node.getBody() == null)
			return false; // parsing error

		node.getBody().accept(loopBody);


		Statement noBody = new EmptyBody(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop

		boolean hasBody = loopBody.getFirst() != null && loopBody.getLast() != null;
		if(hasBody)
			block.mergeWith(loopBody.getBlock().getBody());
		else
			block.addNode(noBody);

		if(hasCondition)
			block.addEdge( new TrueEdge(condition, hasBody ? loopBody.getFirst() : noBody));
		else
			block.addEdge( new TrueEdge(alwaysTrue, hasBody ? loopBody.getFirst() : noBody));

		ParsedBlock updaters = visitSequentialExpressions(node.updaters());
		block.mergeWith(updaters.getBody());

		boolean hasUpdaters= updaters.getBegin() != null && updaters.getEnd() != null;

		Statement noop = new NoOp(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+2)); // added col +2 to avoid conflict with the other noop
		block.addNode(noop);

		block.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, hasUpdaters ? updaters.getBegin() : hasCondition ? condition : alwaysTrue));

		if(hasCondition)
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getEnd() : hasBody ? loopBody.getLast() : noBody, condition));
		else
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getEnd() : hasBody ? loopBody.getLast() : noBody, alwaysTrue));

		if(hasCondition)
			block.addEdge(new FalseEdge(condition, noop));  
		else
			block.addEdge(new FalseEdge(alwaysTrue, noop));  

		// TODO: labels
		ForLoop forloop = new ForLoop(block, hasInitalizers ? initializers.getBody().getNodes() : null, hasCondition ? condition : alwaysTrue, hasUpdaters ? updaters.getBody().getNodes() : null, noop, hasBody ? loopBody.getBlock().getBody().getNodes() : Collections.emptySet());
		this.cfg.getDescriptor().addControlFlowStructure(forloop);
		this.control.endControlFlowOf(block, hasCondition ? condition : alwaysTrue, noop, hasCondition ? condition : alwaysTrue, null);
		this.block = new ParsedBlock(entry, block, noop);
		return false;
	}

	private ParsedBlock visitSequentialExpressions(List<ASTNode> statements) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());
		ASTNode[] stmts = statements.toArray(new ASTNode[statements.size()]);
		Statement prev = null;
		Statement first = null;
		for(int i= 0; i < stmts.length; i++) {
			ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
			stmts[i].accept(visitor);
			Expression expr = visitor.getExpression();
			nodeList.addNode(expr);
			if(i != 0)
				nodeList.addEdge(new SequentialEdge(prev,expr));
			else 
				first = expr;

			prev = expr;
		}
		this.block = new ParsedBlock(first, nodeList, prev);
		return this.block;
	}

	@Override
	public boolean visit(IfStatement node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());
		ExpressionVisitor conditionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);

		if (node.getExpression() == null) {
			return false; // parsing error
		}


		node.getExpression().accept(conditionVisitor);

		Expression condition = conditionVisitor.getExpression();
		nodeList.addNode(condition);

		StatementASTVisitor trueVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);

		if (node.getThenStatement() == null) {
			return false; // parsing error
		}

		node.getThenStatement().accept(trueVisitor);
		ParsedBlock trueBlock = trueVisitor.getBlock();

		boolean isEmptyTrueBlock = trueBlock == null || trueBlock.getBody().getNodes().isEmpty();

		Statement noTrueBody = new EmptyBody(this.cfg, !isEmptyTrueBlock ? condition.getLocation() : new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop


		if(!isEmptyTrueBlock)
			nodeList.mergeWith(trueBlock.getBody());
		else
			nodeList.addNode(noTrueBody);

		nodeList.addEdge(new TrueEdge(condition, !isEmptyTrueBlock ? trueVisitor.getFirst() : noTrueBody));

		Statement noop = new NoOp(cfg, condition.getLocation());
		nodeList.addNode(noop);
		nodeList.addEdge(new SequentialEdge( !isEmptyTrueBlock ? trueVisitor.getLast() : noTrueBody, noop));

		StatementASTVisitor falseVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		ParsedBlock falseBlock = null;

		if (node.getElseStatement() != null) {
			node.getElseStatement().accept(falseVisitor);
			if (node.getElseStatement() != null) {
				falseBlock = falseVisitor.getBlock();
				boolean isEmptyFalseBlock = falseBlock.getBody().getNodes().isEmpty();
				Statement noFalseBody = new EmptyBody(this.cfg, !isEmptyFalseBlock ? condition.getLocation() : new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+2)); // added col + 2 to avoid conflict with the other noop
				if(!isEmptyFalseBlock)
					nodeList.mergeWith(falseBlock.getBody());
				else
					nodeList.addNode(noFalseBody);

				nodeList.addEdge(new FalseEdge(condition, !isEmptyFalseBlock ? falseVisitor.getFirst() : noFalseBody));
				nodeList.addEdge(new SequentialEdge( !isEmptyFalseBlock ? falseVisitor.getLast() : noFalseBody, noop));

			}
		} else {
			nodeList.addEdge(new FalseEdge(condition, noop));
		}

		this.cfg.getDescriptor().addControlFlowStructure(new IfThenElse(nodeList, condition, noop, trueBlock != null ? trueBlock.getBody().getNodes() : Collections.emptyList(), falseBlock != null ? falseBlock.getBody().getNodes() : Collections.emptyList()));
		this.block = new ParsedBlock(condition, nodeList, noop);
		
		return false;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		parserContext.addException(
				new ParsingException("labeled-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Labeled statements are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
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
	public boolean visit(SuperConstructorInvocation node) {
		Unit unit = cfg.getDescriptor().getUnit();
		if (!(unit instanceof ClassUnit)) {
			throw new RuntimeException("The Unit must be a ClassUnit when dealing with SuperConstructorInvocation");
		}
		ClassUnit classUnit = (ClassUnit) unit;
		String superclassName = classUnit.getImmediateAncestors().iterator().next().getName();

		Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this", parserContext.getVariableStaticType(cfg, "this"));
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e  = (ASTNode) args;
				ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
				e.accept(argumentsVisitor);
				Expression expr = argumentsVisitor.getExpression();
				if (expr != null) {
					// This parsing error should be logged in ExpressionVisitor.
					parameters.add(expr);
				}
			}
		}

		Statement call = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null, superclassName, parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);
		
		return false;
	}

	@Override
	public boolean visit(SwitchCase node) {
		ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.accept(exprVisitor);
		Expression expr = exprVisitor.getExpression();

		Statement st = expr != null ? new SwitchEqualityCheck(cfg, getSourceCodeLocation(node), switchItem, expr) :  new SwitchDefault(cfg, getSourceCodeLocation(node));

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(st);
		this.block = new ParsedBlock(st, adj, st);

		return false;
	}

	private Expression switchItem = null;

	@Override
	public boolean visit(SwitchStatement node) {

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(itemVisitor);
		switchItem = itemVisitor.getExpression();

		if(node.statements() == null)
			return false; // parsing error

		Statement noop = new NoOp(this.cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
		adj.addNode(noop);

		List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases = new ArrayList<>();

		DefaultSwitchCase defaultCase = null;
		SwitchDefault switchDefault  = null;

		List<SwitchEqualityCheck> workList= new ArrayList<>();

		List<Statement> instrList= new ArrayList<>();

		int offsetCol = 2;

		Statement first = null, last = null;
		for (Object o : node.statements()) {
			StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg, control, switchItem);
			((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);

			boolean isEmptyBlock = statementASTVisitor.getBlock().getBody().getNodes().isEmpty();
			EmptyBody emptyBlock = null;
			if (isEmptyBlock) {
				emptyBlock = new EmptyBody(cfg, getSourceCodeLocation(node));
				adj.addNode(emptyBlock);
			} else {
				adj.mergeWith(statementASTVisitor.getBlock().getBody());
				instrList.addAll(statementASTVisitor.getBlock().getBody().getNodes());
			}

			if(o instanceof SwitchCase) {
				if(statementASTVisitor.getFirst() instanceof SwitchEqualityCheck)
					workList.add((SwitchEqualityCheck) statementASTVisitor.getFirst());
				else if(statementASTVisitor.getFirst() instanceof SwitchDefault) {
					switchDefault = (SwitchDefault) statementASTVisitor.getFirst();
				}  		
			} else if(o instanceof BreakStatement) {
				for(SwitchEqualityCheck switchCondition : workList) {

					adj.addEdge(new TrueEdge(switchCondition, getFirstInstructionAfterSwitchInstr(switchCondition, instrList)));
					cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, instrList));

				}

				if(switchDefault != null) {
					defaultCase = new DefaultSwitchCase(switchDefault, instrList);
					Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, instrList);
					if(follower != null) {
						adj.addEdge(new SequentialEdge(switchDefault, follower));
					} else {
						emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
						offsetCol++;
						adj.addNode(emptyBlock);
						adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
					}
				}

				workList = new ArrayList<>();
				instrList = new ArrayList<>();        		
			} 

			if (first == null) {
				first = isEmptyBlock ? emptyBlock : statementASTVisitor.getFirst();
			}
			if (last != null) {
				if(!(last instanceof SwitchEqualityCheck || last instanceof SwitchDefault))
					adj.addEdge(new SequentialEdge(last, statementASTVisitor.getFirst()));
			}
			last = statementASTVisitor.getLast();
		} 

		EmptyBody emptyBlock = null;

		if(switchDefault != null && defaultCase == null) {
			defaultCase = new DefaultSwitchCase(switchDefault, instrList);
			Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, instrList);
			if(follower != null) {
				adj.addEdge(new SequentialEdge(switchDefault, follower));
				adj.addEdge(new SequentialEdge(instrList.getLast(), noop));
			} else {
				emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
				offsetCol++;
				adj.addNode(emptyBlock);
				adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
				adj.addEdge(new SequentialEdge(emptyBlock, noop));
			}
		}

		for(SwitchEqualityCheck switchCondition : workList) {

			if(instrList.size() > 0) {
				adj.addEdge(new TrueEdge(switchCondition,instrList.getFirst()));
			} else {
				emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
				offsetCol++;
				adj.addNode(emptyBlock);
				adj.addEdge(new TrueEdge(defaultCase.getEntry(),emptyBlock));
			}

			cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, instrList));
			if(instrList.size() > 0 )
				offsetCol++;		
		}

		for(int i = 0; i < cases.size()-1; i++)
			adj.addEdge(new FalseEdge(cases.get(i).getCondition(), cases.get(i+1).getCondition()));


		if(defaultCase != null && !cases.isEmpty())
			adj.addEdge(new FalseEdge(cases.getLast().getCondition(), defaultCase.getEntry()));


		lazySwitchEdgeBindingCleanUp(adj, cases, defaultCase != null ? defaultCase.getEntry() : null);

		if(node.statements().isEmpty() || (cases.isEmpty() && defaultCase == null)) {
			emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
			adj.addNode(emptyBlock);
			adj.addEdge(new SequentialEdge(emptyBlock, noop));
			first = emptyBlock;
		}

		//TODO: labels
		this.cfg.getDescriptor().addControlFlowStructure(new Switch(adj, !cases.isEmpty() ? cases.getFirst().getCondition() : defaultCase != null ? defaultCase.getEntry() : emptyBlock, noop, cases.toArray(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase[cases.size()]), defaultCase)); 
		this.control.endControlFlowOf(adj, !cases.isEmpty() ? cases.getFirst().getCondition() : defaultCase != null ? defaultCase.getEntry() : emptyBlock, noop, null, null);
		this.block = new ParsedBlock(first, adj, noop);
		return false;
	}

	private void lazySwitchEdgeBindingCleanUp(NodeList<CFG, Statement, Edge> adj, List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases, SwitchDefault switchDefault) {
		for(it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase c : cases) {
			for(Edge e : adj.getEdges()) {
				if(e.getDestination().equals(c.getCondition()) 
						&& !( e instanceof FalseEdge && (e.getSource() instanceof SwitchEqualityCheck || e.getSource() instanceof SwitchDefault))) {
					adj.removeEdge(e);
					adj.addEdge(e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(adj, c.getCondition())));
				} 
			}
		}

		if(switchDefault != null) {
			for(Edge e : adj.getEdges()) {
				if(e.getDestination().equals(switchDefault) 
						&& !( e instanceof FalseEdge && e.getSource() instanceof SwitchEqualityCheck)) {
					adj.removeEdge(e);
					adj.addEdge(e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(adj, switchDefault)));
				}
			}
		}
	}

	private Statement getFirstInstructionAfterSwitchInstr(NodeList<CFG, Statement, Edge> adj, Statement stmt) {
		return getFirstInstructionAfterSwitchInstrRecursive(adj, stmt, new HashSet<>());
	}

	private Statement getFirstInstructionAfterSwitchInstrRecursive(NodeList<CFG, Statement, Edge> block, Statement stmt, Set<Statement> seen) {

		if(!seen.contains(stmt)) {
			seen.add(stmt);


			List<TrueEdge> trueSwitchCaseEdges = new ArrayList<>();
			List<FalseEdge> falseSwitchCaseEdges = new ArrayList<>();
			List<SequentialEdge> defaultEdges = new ArrayList<>();

			for(Edge e : block.getEdges()) {
				if(e.getSource().equals(stmt)) {
					if(! (e.getDestination() instanceof SwitchEqualityCheck || e.getDestination() instanceof SwitchDefault) ) {
						return e.getDestination();
					} else if(stmt instanceof SwitchEqualityCheck) {
						if(e instanceof TrueEdge)
							trueSwitchCaseEdges.add((TrueEdge) e);
						else if(e instanceof FalseEdge)
							falseSwitchCaseEdges.add((FalseEdge) e);
					} else if(stmt instanceof SwitchDefault) {
						defaultEdges.add((SequentialEdge) e);
					}
				}
			}

			for(TrueEdge e : trueSwitchCaseEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if(res != null)
					return res;
			}

			for(FalseEdge e : falseSwitchCaseEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if(res != null)
					return res;
			}

			for(SequentialEdge e : defaultEdges) {
				Statement res = getFirstInstructionAfterSwitchInstrRecursive(block, e.getDestination(), seen);
				if(res != null)
					return res;
			}
		}
		return null;	
	}

	private Statement getFirstInstructionAfterSwitchInstr(Statement stmt, List<Statement> instrList) {
		Iterator<Statement> iter = instrList.iterator();

		boolean found = false;
		while(iter.hasNext() && !found) {
			if(iter.next().equals(stmt))
				found = true;
		}

		if(found) {
			while(iter.hasNext()) {
				Statement next = iter.next();
				if(!(next instanceof SwitchDefault || next instanceof SwitchEqualityCheck))
					return next;
			}
		}

		return null;
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		parserContext.addException(
				new ParsingException("synchronized-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Synchronized statements are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		parserContext.addException(
				new ParsingException("type-declaration-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Type declaration statements are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		Statement first = null, last = null;
		TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		node.getType().accept(visitor);
		Type variableType = visitor.getType();
		if (variableType.isInMemoryType()) {
			variableType = new ReferenceType(variableType);
		}
		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			SourceCodeLocation loc = getSourceCodeLocation(fragment);
			VariableRef ref = new VariableRef(cfg,
					getSourceCodeLocation(fragment),
					variableName, variableType);
			Expression initializer;
			parserContext.addVariableType(cfg,variableName, variableType);
			if(fragment.getInitializer() == null) {
				initializer = JavaTypeSystem.getDefaultLiteral(variableType, cfg, loc);
			} else {
				org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
				ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
				expr.accept(exprVisitor);
				initializer = exprVisitor.getExpression();
				if (initializer == null) {
					initializer = new NullLiteral(cfg, loc);
				}
			}
			JavaAssignment assignment = new JavaAssignment(cfg, loc, ref, initializer);
			adj.addNode(assignment);
			if (getFirst() == null) {
				first = assignment;
			} else {
				// FIXME: first? not last?
				adj.addEdge(new SequentialEdge(first, assignment));
			}
			
			last = assignment;

		}
		
		this.block = new ParsedBlock(first, adj, last);
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		ExpressionVisitor condition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(condition);
		Expression expression = condition.getExpression();

		if (expression == null) {
			// Parsing error. Skipping...
			return false;
		}

		adj.addNode(expression);

		StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		node.getBody().accept(loopBody);

		if(node.getBody() == null)
			return false; // parsing error

		Statement noBody = new EmptyBody(this.cfg, getSourceCodeLocation(node));

		boolean hasBody = loopBody.getFirst() != null && loopBody.getLast() != null;
		if(hasBody) {
			adj.mergeWith(loopBody.getBlock().getBody());
		} else {
			adj.addNode(noBody);
		}

		adj.addEdge(new TrueEdge(expression, hasBody ? loopBody.getFirst() : noBody));
		adj.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, expression));

		Statement noop = new NoOp(this.cfg, expression.getLocation());
		adj.addNode(noop);
		adj.addEdge(new FalseEdge(expression, noop));

		// TODO: labels
		WhileLoop whileLoop = new WhileLoop(adj, expression, noop, hasBody ? loopBody.getBlock().getBody().getNodes() : Collections.emptySet());
		this.cfg.getDescriptor().addControlFlowStructure(whileLoop);
		this.control.endControlFlowOf(adj, expression, noop, expression, null);
		this.block = new ParsedBlock(expression, adj, noop);
				
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		ExpressionVisitor exprVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getExpression().accept(exprVisitor);
		Expression expr = exprVisitor.getExpression();
		Throw th = new Throw(cfg, getSourceCodeLocation(node), expr);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(th);
		this.block = new ParsedBlock(th, adj, th);
		return false;
	}

	@Override
	public boolean visit(TryStatement node) {
		NodeList<CFG, Statement, Edge> trycatch = new NodeList<>(new SequentialEdge());

		// normal exit points of the try-catch in case there is no finally
		// block:
		// in this case, we have to add a noop at the end of the whole try-catch
		// to use it as unique exit point in the returned triple
		Collection<Statement> normalExits = new LinkedList<>();

		// we parse the body of the try block normally
		StatementASTVisitor blockVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		node.getBody().accept(blockVisitor);
		// body of the try
		ParsedBlock body = blockVisitor.getBlock();
		trycatch.mergeWith(body.getBody());

		// if there is an else block, we parse it immediately and connect it
		// to the end of the try block *only* if it does not end with a
		// return/throw
		// (as it would be deadcode)
		normalExits.add(body.getEnd());


		// we then parse each catch block, and we connect *every* instruction
		// that can end the try block to the beginning of each catch block
		List<CatchBlock> catches = new LinkedList<>();
		List<ParsedBlock> catchBodies = new LinkedList<>();
		for (int i = 0; i < node.catchClauses().size(); i++) {
			CatchClause cl = (CatchClause) node.catchClauses().get(i);
			StatementASTVisitor clVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
			cl.accept(clVisitor);

			CatchBlock block =  clVisitor.clBlock;
			ParsedBlock visit =  clVisitor.getBlock();
			catches.add(block);
			catchBodies.add(visit);
			trycatch.mergeWith(visit.getBody());

			if (body.canBeContinued())
				trycatch.addEdge(
						new ErrorEdge(body.getEnd(), visit.getBegin(), block.getIdentifier(), block.getExceptions()));
			for (Statement st : body.getBody().getNodes())
				if (st.stopsExecution())
					trycatch.addEdge(new ErrorEdge(st, visit.getBegin(), block.getIdentifier(), block.getExceptions()));

			if (visit.canBeContinued())
				// non-stopping last statement
				normalExits.add(visit.getEnd());
		}

		// lastly, we parse the finally block and
		// we connect it with the body (or the else block if it exists) and with
		// each catch block
		ParsedBlock finallyBlock = null;
		if (node.getFinally() != null) {
			StatementASTVisitor finallyVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
			node.getFinally().accept(finallyVisitor);
			finallyBlock =  finallyVisitor.getBlock();			
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
					trycatch.addEdge(new SequentialEdge(st, noop));
		}


		// build protection block
		this.cfg.getDescriptor().addProtectionBlock(
				new ProtectionBlock(
						new ProtectedBlock(body.getBegin(), body.getEnd(), body.getBody().getNodes()),
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
	public boolean visit(CatchClause node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
		ExpressionVisitor paramVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
		node.getException().getType().accept(typeVisitor);
		node.getException().getName().accept(paramVisitor);

		// type of the exception		
		Type type = typeVisitor.getType();
		// exception param
		Expression param = paramVisitor.getExpression();

		StatementASTVisitor catchBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		node.getBody().accept(catchBody);
		// body of the catch clause
		ParsedBlock body = catchBody.getBlock();


		CatchBlock catchBlock = new CatchBlock(
				(VariableRef) param,
				new ProtectedBlock(body.getBegin(), body.getEnd(), body.getBody().getNodes()),
				type);

		this.clBlock = catchBlock;
		this.block = body;
		return false;
	}
}
