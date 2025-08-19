package it.unive.jlisa.frontend.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
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

public class StatementASTVisitor extends JavaASTVisitor {

	private CFG cfg;

	private Statement first;

	private Statement last;

	private NodeList<CFG, it.unive.lisa.program.cfg.statement.Statement, Edge> block = new NodeList<>(new SequentialEdge());

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
		return first;
	}

	public Statement getLast() {
		return last;
	}

	public NodeList<CFG, Statement, Edge> getBlock() {
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

		block.addNode(assrt);

		first = assrt;
		last = assrt;

		return false;
	}

	@Override
	public boolean visit(Block node) {
		block = new NodeList<>(new SequentialEdge());

		for (Object o : node.statements()) {
			StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg, this.control);
			((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);

			boolean isEmptyBlock = statementASTVisitor.getBlock().getNodes().isEmpty();
			EmptyBody emptyBlock = null;
			if (isEmptyBlock) {
				new EmptyBody(cfg, getSourceCodeLocation(node));
				block.addNode(emptyBlock);
			} else {
				block.mergeWith(statementASTVisitor.getBlock());
			}

			if (first == null) {
				first = isEmptyBlock ? emptyBlock : statementASTVisitor.getFirst();
			}

			if (last != null) {
				block.addEdge(new SequentialEdge(last, statementASTVisitor.getFirst()));
			}
			last = statementASTVisitor.getLast();
		}
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		JavaBreak brk = new JavaBreak(cfg, getSourceCodeLocation(node));
		block.addNode(brk);

		//TODO: labels
		control.addModifier(brk);

		first = brk;
		last = brk;    	
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
		first = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null,this.cfg.getDescriptor().getName(), parameters.toArray(new Expression[0]));
		last = first;
		block.addNode(first);

		return false;
	}

	@Override
	public boolean visit(ContinueStatement node) {

		JavaContinue cnt = new JavaContinue(cfg, getSourceCodeLocation(node));
		block.addNode(cnt);
		// TODO: labels
		control.addModifier(cnt);

		first = cnt;
		last = cnt;

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

		boolean hasBody = doBody.getFirst() != null && doBody.getLast() != null;
		if(hasBody) {
			block.mergeWith(doBody.getBlock());
			this.first = doBody.getFirst();
		} else {
			block.addNode(noBody);
			this.first = noBody;
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

		this.last = noop;
		this.block = block;

		// TODO: labels
		DoWhileLoop doWhileLoop = new DoWhileLoop(block, expression, noop, doBody.getBlock().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(doWhileLoop);
		this.control.endControlFlowOf(block, expression, noop, expression, null);

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
		this.first = condition;

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
			block.mergeWith(loopBody.getBlock());
		else
			block.addNode(noBody);

		block.addEdge( new SequentialEdge(assignment, hasBody ? loopBody.getFirst() : noBody));

		block.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, condition));

		Statement noop = new NoOp(this.cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
		block.addNode(noop);

		block.addEdge(new FalseEdge(condition, noop));

		this.last = noop;
		this.block = block;

		// TODO: labels
		ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop, loopBody.getBlock().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(forEachLoop);
		this.control.endControlFlowOf(block, condition, noop, condition, null);

		return false;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		block = new NodeList<>(new SequentialEdge());
		ExpressionVisitor expressionVisitor = new ExpressionVisitor(parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(expressionVisitor);
		first = expressionVisitor.getExpression();
		if (first == null) {
			// PARSING ERROR. IGNORE
			return false;
		}
		first = expressionVisitor.getExpression();
		last = first;
		block.addNode(first);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(ForStatement node) {

		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> initializers = visitSequentialExpressions(node.initializers());

		boolean hasInitalizers = initializers.getLeft() != null && initializers.getRight() != null;
		NoOp noInit = new NoOp(cfg, getSourceCodeLocation(node));
		if(hasInitalizers) {
			block.mergeWith(initializers.getMiddle());
			this.first = initializers.getLeft();
		} else {
			block.addNode(noInit);
			this.first = noInit;
		}

		ExpressionVisitor conditionExpr = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		if(node.getExpression() != null)
			node.getExpression().accept(conditionExpr);
		Expression condition = conditionExpr.getExpression();
		Statement alwaysTrue = new Equal(cfg, getSourceCodeLocation(node), new TrueLiteral(cfg, getSourceCodeLocation(node)), new TrueLiteral(cfg, getSourceCodeLocation(node)));

		boolean hasCondition = condition != null;

		if(hasCondition) {
			block.addNode(condition);
			block.addEdge(new SequentialEdge(hasInitalizers? initializers.getRight() : noInit, condition));
		} else {
			block.addNode(alwaysTrue);
			block.addEdge(new SequentialEdge(hasInitalizers? initializers.getRight() : noInit, alwaysTrue));
		}

		StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		if(node.getBody() == null)
			return false; // parsing error

		node.getBody().accept(loopBody);


		Statement noBody = new EmptyBody(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop

		boolean hasBody = loopBody.getFirst() != null && loopBody.getLast() != null;
		if(hasBody)
			block.mergeWith(loopBody.getBlock());
		else
			block.addNode(noBody);

		if(hasCondition)
			block.addEdge( new TrueEdge(condition, hasBody ? loopBody.getFirst() : noBody));
		else
			block.addEdge( new TrueEdge(alwaysTrue, hasBody ? loopBody.getFirst() : noBody));

		Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> updaters = visitSequentialExpressions(node.updaters());
		block.mergeWith(updaters.getMiddle());

		boolean hasUpdaters= updaters.getLeft() != null && updaters.getRight() != null;

		Statement noop = new NoOp(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+2)); // added col +2 to avoid conflict with the other noop
		block.addNode(noop);

		block.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, hasUpdaters ? updaters.getLeft() : hasCondition ? condition : alwaysTrue));

		if(hasCondition)
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getRight() : hasBody ? loopBody.getLast() : noBody, condition));
		else
			block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getRight() : hasBody ? loopBody.getLast() : noBody, alwaysTrue));

		if(hasCondition)
			block.addEdge(new FalseEdge(condition, noop));  
		else
			block.addEdge(new FalseEdge(alwaysTrue, noop));  

		this.last = noop;
		this.block = block;

		// TODO: labels

		ForLoop forloop = new ForLoop(block, hasInitalizers ? initializers.getMiddle().getNodes() : null, hasCondition ? condition : alwaysTrue, hasUpdaters ? updaters.getMiddle().getNodes() : null, noop, loopBody.getBlock().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(forloop);
		this.control.endControlFlowOf(block, hasCondition ? condition : alwaysTrue, noop, hasCondition ? condition : alwaysTrue, null);

		return false;
	}

	private Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> visitSequentialExpressions(List<ASTNode> statements) {
		block = new NodeList<>(new SequentialEdge());
		ASTNode[] stmts = statements.toArray(new ASTNode[statements.size()]);
		Statement prev = null;
		Statement first = null;
		for(int i= 0; i < stmts.length; i++) {
			ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
			stmts[i].accept(visitor);
			Expression expr = visitor.getExpression();
			block.addNode(expr);
			if(i != 0)
				block.addEdge(new SequentialEdge(prev,expr));
			else {
				block.getEntries().add(expr);
				first = expr;
			}
			prev = expr;
		}
		return Triple.of(first, block, prev);
	}

	@Override
	public boolean visit(IfStatement node) {
		block = new NodeList<>(new SequentialEdge());
		ExpressionVisitor conditionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);

		if (node.getExpression() == null) {
			return false; // parsing error
		}


		node.getExpression().accept(conditionVisitor);

		Expression condition = conditionVisitor.getExpression();
		first = condition;
		block.addNode(condition);

		StatementASTVisitor trueVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);

		if (node.getThenStatement() == null) {
			return false; // parsing error
		}

		node.getThenStatement().accept(trueVisitor);
		NodeList<CFG, Statement, Edge> trueBlock = trueVisitor.getBlock();

		boolean isEmptyTrueBlock = trueBlock.getNodes().isEmpty();

		Statement noTrueBody = new EmptyBody(this.cfg, !isEmptyTrueBlock ? condition.getLocation() : new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop


		if(!isEmptyTrueBlock)
			block.mergeWith(trueBlock);
		else
			block.addNode(noTrueBody);

		block.addEdge(new TrueEdge(condition, !isEmptyTrueBlock ? trueVisitor.getFirst() : noTrueBody));

		Statement noop = new NoOp(cfg, condition.getLocation());
		block.addNode(noop);
		block.addEdge(new SequentialEdge( !isEmptyTrueBlock ? trueVisitor.getLast() : noTrueBody, noop));

		StatementASTVisitor falseVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		NodeList<CFG, Statement, Edge> falseBlock = null;

		if (node.getElseStatement() != null) {
			node.getElseStatement().accept(falseVisitor);
			if (node.getElseStatement() != null) {
				falseBlock = falseVisitor.getBlock();
				boolean isEmptyFalseBlock = falseBlock.getNodes().isEmpty();
				Statement noFalseBody = new EmptyBody(this.cfg, !isEmptyFalseBlock ? condition.getLocation() : new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+2)); // added col + 2 to avoid conflict with the other noop
				if(!isEmptyFalseBlock)
					block.mergeWith(falseBlock);
				else
					block.addNode(noFalseBody);

				block.addEdge(new FalseEdge(condition, !isEmptyFalseBlock ? falseVisitor.getFirst() : noFalseBody));
				block.addEdge(new SequentialEdge( !isEmptyFalseBlock ? falseVisitor.getLast() : noFalseBody, noop));

			}
		} else {
			block.addEdge(new FalseEdge(condition, noop));
		}

		last = noop;

		this.cfg.getDescriptor().addControlFlowStructure(new IfThenElse(block, condition, noop, trueBlock.getNodes(), falseBlock != null ? falseBlock.getNodes() : Collections.emptyList()));

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
		first = ret;
		last = ret;
		block.addNode(ret);
		/*parserContext.addException(
                new ParsingException("return-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Return statements are not supported.",
                        getSourceCodeLocation(node))
        );*/
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
		first = call;
		last = call;
		block.addNode(call);
		/*parserContext.addException(
                new ParsingException("super-constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Super constructor invocations are not supported.",
                        getSourceCodeLocation(node))
        );*/
		return false;
	}

	@Override
	public boolean visit(SwitchCase node) {

		ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.accept(exprVisitor);
		Expression expr = exprVisitor.getExpression();

		Statement st = expr != null ? new SwitchEqualityCheck(cfg, getSourceCodeLocation(node), switchItem, expr) :  new SwitchDefault(cfg, getSourceCodeLocation(node));

		block.addNode(st);

		first = st;
		last = st;

		return false;
	}

	private Expression switchItem = null;

	@Override
	public boolean visit(SwitchStatement node) {

		block = new NodeList<>(new SequentialEdge());

		ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(itemVisitor);
		switchItem = itemVisitor.getExpression();

		if(node.statements() == null)
			return false; // parsing error

		Statement noop = new NoOp(this.cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
		block.addNode(noop);

		List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases = new ArrayList<>();

		DefaultSwitchCase defaultCase = null;
		SwitchDefault switchDefault  = null;

		List<SwitchEqualityCheck> workList= new ArrayList<>();

		List<Statement> instrList= new ArrayList<>();

		int offsetCol = 2;

		for (Object o : node.statements()) {
			StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg, control, switchItem);
			((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);

			boolean isEmptyBlock = statementASTVisitor.getBlock().getNodes().isEmpty();
			EmptyBody emptyBlock = null;
			if (isEmptyBlock) {
				emptyBlock = new EmptyBody(cfg, getSourceCodeLocation(node));
				block.addNode(emptyBlock);
			} else {
				block.mergeWith(statementASTVisitor.getBlock());
				instrList.addAll(statementASTVisitor.getBlock().getNodes());
			}

			if(o instanceof SwitchCase) {
				if(statementASTVisitor.getFirst() instanceof SwitchEqualityCheck)
					workList.add((SwitchEqualityCheck) statementASTVisitor.getFirst());
				else if(statementASTVisitor.getFirst() instanceof SwitchDefault) {
					switchDefault = (SwitchDefault) statementASTVisitor.getFirst();
				}  		
			} else if(o instanceof BreakStatement) {
				for(SwitchEqualityCheck switchCondition : workList) {

					block.addEdge(new TrueEdge(switchCondition, getFirstInstructionAfterSwitchInstr(switchCondition, instrList)));
					cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, instrList));

				}

				if(switchDefault != null) {
					defaultCase = new DefaultSwitchCase(switchDefault, instrList);
					Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, instrList);
					if(follower != null) {
						block.addEdge(new SequentialEdge(switchDefault, follower));
					} else {
						emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
						offsetCol++;
						block.addNode(emptyBlock);
						block.addEdge(new SequentialEdge(switchDefault, emptyBlock));
					}
				}

				workList = new ArrayList<>();
				instrList = new ArrayList<>();        		
			} 

			if (first == null) {
				first = isEmptyBlock ? emptyBlock : statementASTVisitor.getFirst();
			}
			if (last != null) {
				if (!(last instanceof SwitchEqualityCheck || last instanceof SwitchDefault))
					block.addEdge(new SequentialEdge(last, statementASTVisitor.getFirst()));
			}
			last = statementASTVisitor.getLast();
		} 

		EmptyBody emptyBlock = null;

		if(switchDefault != null && defaultCase == null) {
			defaultCase = new DefaultSwitchCase(switchDefault, instrList);
			Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, instrList);
			if(follower != null) {
				block.addEdge(new SequentialEdge(switchDefault, follower));
				block.addEdge(new SequentialEdge(instrList.getLast(), noop));
			} else {
				emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
				offsetCol++;
				block.addNode(emptyBlock);
				block.addEdge(new SequentialEdge(switchDefault, emptyBlock));
				block.addEdge(new SequentialEdge(emptyBlock, noop));
			}
		}

		for(SwitchEqualityCheck switchCondition : workList) {

			if(instrList.size() > 0) {
				block.addEdge(new TrueEdge(switchCondition,instrList.getFirst()));
			} else {
				emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
				offsetCol++;
				block.addNode(emptyBlock);
				block.addEdge(new TrueEdge(defaultCase.getEntry(),emptyBlock));
			}

			cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, instrList));
			if(instrList.size() > 0 )
				offsetCol++;		
		}

		for(int i = 0; i < cases.size()-1; i++)
			block.addEdge(new FalseEdge(cases.get(i).getCondition(), cases.get(i+1).getCondition()));


		if(defaultCase != null && !cases.isEmpty())
			block.addEdge(new FalseEdge(cases.getLast().getCondition(), defaultCase.getEntry()));


		lazySwitchEdgeBindingCleanUp(cases, defaultCase != null ? defaultCase.getEntry() : null);

		if(node.statements().isEmpty() || (cases.isEmpty() && defaultCase == null)) {
			emptyBlock = new EmptyBody(cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+offsetCol));
			block.addNode(emptyBlock);
			block.addEdge(new SequentialEdge(emptyBlock, noop));
			first = emptyBlock;
		}

		this.last = noop;

		//TODO: labels
		this.cfg.getDescriptor().addControlFlowStructure(new Switch(block, !cases.isEmpty() ? cases.getFirst().getCondition() : defaultCase != null ? defaultCase.getEntry() : emptyBlock, noop, cases.toArray(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase[cases.size()]), defaultCase)); 
		this.control.endControlFlowOf(block, !cases.isEmpty() ? cases.getFirst().getCondition() : defaultCase != null ? defaultCase.getEntry() : emptyBlock, noop, null, null);
		return false;
	}

	private void lazySwitchEdgeBindingCleanUp(List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases, SwitchDefault switchDefault) {
		for(it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase c : cases) {
			for(Edge e : block.getEdges()) {
				if(e.getDestination().equals(c.getCondition()) 
						&& !( e instanceof FalseEdge && (e.getSource() instanceof SwitchEqualityCheck || e.getSource() instanceof SwitchDefault))) {
					block.removeEdge(e);
					block.addEdge(e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(block, c.getCondition())));
				} 
			}
		}

		if(switchDefault != null) {
			for(Edge e : block.getEdges()) {
				if(e.getDestination().equals(switchDefault) 
						&& !( e instanceof FalseEdge && e.getSource() instanceof SwitchEqualityCheck)) {
					block.removeEdge(e);
					block.addEdge(e.newInstance(e.getSource(), getFirstInstructionAfterSwitchInstr(block, switchDefault)));
				}
			}
		}
	}

	private Statement getFirstInstructionAfterSwitchInstr(NodeList<CFG, Statement, Edge> block, Statement stmt) {

		return getFirstInstructionAfterSwitchInstrRecursive(block, stmt, new HashSet<>());
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
		block = new NodeList<>(new SequentialEdge());
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
			block.addNode(assignment);
			if (first == null) {
				first = assignment;
			} else {
				block.addEdge(new SequentialEdge(first, assignment));
			}
			//cfg.getNodeList().mergeWith(block);
			//cfg.addNode(ref);
			last = assignment;

			//fragment.getInitializer()
		}
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

		ExpressionVisitor condition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
		node.getExpression().accept(condition);
		Expression expression = condition.getExpression();

		if (expression == null) {
			// Parsing error. Skipping...
			return false;
		}

		this.first = expression;
		block.addNode(expression);

		StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		node.getBody().accept(loopBody);

		if(node.getBody() == null)
			return false; // parsing error

		Statement noBody = new EmptyBody(this.cfg, getSourceCodeLocation(node));

		boolean hasBody = loopBody.getFirst() != null && loopBody.getLast() != null;
		if(hasBody) {
			block.mergeWith(loopBody.getBlock());
		} else {
			block.addNode(noBody);
		}

		block.addEdge(new TrueEdge(expression, hasBody ? loopBody.getFirst() : noBody));
		block.addEdge(new SequentialEdge(hasBody ? loopBody.getLast() : noBody, expression));

		Statement noop = new NoOp(this.cfg, expression.getLocation());
		block.addNode(noop);
		block.addEdge(new FalseEdge(expression, noop));

		this.last = noop;
		this.block = block;

		// TODO: labels
		WhileLoop whileLoop = new WhileLoop(block, expression, noop, loopBody.getBlock().getNodes());
		this.cfg.getDescriptor().addControlFlowStructure(whileLoop);
		this.control.endControlFlowOf(block, expression, noop, expression, null);

		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		parserContext.addException(
				new ParsingException("throw-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Throw statements are not supported.",
						getSourceCodeLocation(node))
				);
		return false;
	}

	@Override
	public boolean visit(TryStatement node) {
		NodeList<CFG, Statement, Edge> trycatch = new NodeList<>(new SequentialEdge());
		System.err.println(node.getFinally());
		// normal exit points of the try-catch in case there is no finally
		// block:
		// in this case, we have to add a noop at the end of the whole try-catch
		// to use it as unique exit point in the returned triple
		Collection<Statement> normalExits = new LinkedList<>();

		// we parse the body of the try block normally
		StatementASTVisitor blockVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
		node.getBody().accept(blockVisitor);
		// body of the try
		NodeList<CFG, Statement, Edge> body = blockVisitor.getBlock();

		trycatch.mergeWith(body);

		// if there is an else block, we parse it immediately and connect it
		// to the end of the try block *only* if it does not end with a
		// return/throw
		// (as it would be deadcode)
		normalExits.add(body.getExits().stream().findFirst().get());


		// we then parse each catch block, and we connect *every* instruction
		// that can end the try block to the beginning of each catch block
		List<CatchBlock> catches = new LinkedList<>();
		List<NodeList<CFG, Statement, Edge>> catchBodies = new LinkedList<>();
		for (int i = 0; i < node.catchClauses().size(); i++) {
			CatchClause cl = (CatchClause) node.catchClauses().get(i);
			StatementASTVisitor clVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
			cl.accept(clVisitor);

			CatchBlock block =  clVisitor.clBlock;
			NodeList<CFG, Statement, Edge> visit =  clVisitor.block;
			catches.add(block);
			catchBodies.add(visit);
			trycatch.mergeWith(visit);

			Statement end = body.getExits().stream().findFirst().get();
			Statement entry = visit.getEntries().stream().findFirst().get();

			if (end != null && !end.stopsExecution() && !end.breaksControlFlow() && !end.continuesControlFlow())
				trycatch.addEdge(
						new ErrorEdge(end, entry, block.getIdentifier(), block.getExceptions()));
			for (Statement st : body.getNodes())
				if (st.stopsExecution())
					trycatch.addEdge(new ErrorEdge(st, entry, block.getIdentifier(), block.getExceptions()));

			Statement visitEnd = visit.getExits().stream().findFirst().get();

			if (visitEnd != null && !visitEnd.stopsExecution() && !visitEnd.breaksControlFlow() && !visitEnd.continuesControlFlow())
				// non-stopping last statement
				normalExits.add(visitEnd);
		}

		// lastly, we parse the finally block and
		// we connect it with the body (or the else block if it exists) and with
		// each catch block

		//TODO: final
		NodeList<CFG, Statement, Edge> finallyBlock = null;
		if (node.getFinally() != null) {
			StatementASTVisitor finallyVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg, this.control);
			node.getFinally().accept(finallyVisitor);
			finallyBlock =  finallyVisitor.getBlock();			
			trycatch.mergeWith(finallyBlock);
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

		Statement end = body.getExits().stream().findFirst().get();
		Statement entry = body.getEntries().stream().findFirst().get();

		// build protection block
		this.cfg.getDescriptor().addProtectionBlock(
				new ProtectionBlock(
						new ProtectedBlock(entry, end, body.getNodes()),
						catches,
						null,
						finallyBlock == null ? null
								: new ProtectedBlock(
										finallyBlock.getEntries().stream().findFirst().get(),
										finallyBlock.getExits().stream().findFirst().get(),
										finallyBlock.getNodes()),
								usedNoop ? noop : null));

		this.block = trycatch;
		this.first = entry;
		this.last = usedNoop ? noop : null;
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
		this.block = catchBody.getBlock();


		CatchBlock catchBlock = new CatchBlock(
				(VariableRef) param,
				new ProtectedBlock(block.getEntries().stream().findFirst().get(), block.getExits().stream().findFirst().get(), block.getNodes()),
				type);

		this.clBlock = catchBlock;
		this.last = block.getExits().stream().findFirst().get();
		return false;
	}
}
