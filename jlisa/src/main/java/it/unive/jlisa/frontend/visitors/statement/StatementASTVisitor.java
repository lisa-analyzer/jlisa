package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.loops.SynchronizedBlock;
import it.unive.jlisa.program.cfg.expression.JavaCastExpression;
import it.unive.jlisa.program.cfg.expression.JavaNewArrayWithInitializer;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.expression.JavaUnresolvedCall;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaThrow;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaBreak;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaContinue;
import it.unive.jlisa.program.cfg.statement.literal.JavaNullLiteral;
import it.unive.jlisa.program.cfg.statement.literal.JavaStringLiteral;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
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
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class StatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(StatementASTVisitor.class);
	private ParsedBlock block;

	public StatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
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
	public ParsedBlock getResult() {
		return block;
	}

	@Override
	public boolean visit(
			AssertStatement node) {

		org.eclipse.jdt.core.dom.Expression expr = node.getExpression();
		org.eclipse.jdt.core.dom.Expression msg = node.getMessage();

		Expression expression1 = getParserContext().evaluate(expr,
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		Statement assrt = null;
		if (msg != null) {
			Expression expression2 = getParserContext().evaluate(msg,
					() -> new ExpressionVisitor(getEnvironment(), getScope()));
			assrt = new AssertionStatement(getScope().getCFG(), getSourceCodeLocation(node), expression1, expression2);

		} else {
			assrt = new SimpleAssert(getScope().getCFG(), getSourceCodeLocation(node), expression1);
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

		getScope().getTracker().enterScope();

		Statement first = null, last = null;
		if (node.statements().isEmpty()) { // empty block
			NoOp emptyBlock = new NoOp(getScope().getCFG(), getSourceCodeLocation(node));
			nodeList.addNode(emptyBlock);
			first = emptyBlock;
			last = emptyBlock;
		} else {
			for (Object o : node.statements()) {
				ParsedBlock stmtBlock = getParserContext().evaluate(
						(org.eclipse.jdt.core.dom.Statement) o,
						() -> new StatementASTVisitor(getEnvironment(), getScope()));

				nodeList.mergeWith(stmtBlock.getBody());

				if (first == null)
					first = stmtBlock.getBegin();

				if (last != null)
					nodeList.addEdge(new SequentialEdge(last, stmtBlock.getBegin()));

				last = stmtBlock.getEnd();
			}
		}
		getScope().getTracker().exitScope(last);

		this.block = new ParsedBlock(first, nodeList, last);
		return false;
	}

	@Override
	public boolean visit(
			BreakStatement node) {
		JavaBreak br = new JavaBreak(getScope().getCFG(), getSourceCodeLocation(node));

		// TODO: labels
		getScope().getControlFlowTracker().addModifier(br);

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
		Expression thisExpression = new VariableRef(getScope().getCFG(),
				getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(), "this");
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);
		if (getScope().getParentScope().getEnclosingClass() != null) {
			Expression enclExpression = new VariableRef(getScope().getCFG(),
					getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
					"$enclosing");
			parameters.add(enclExpression);
		}

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				Expression expr = getParserContext().evaluate(e,
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
				parameters.add(expr);
			}
		}
		JavaUnresolvedCall call = new JavaUnresolvedCall(getScope().getCFG(),
				getSourceCodeLocationManager(node, true).nextColumn(),
				Call.CallType.INSTANCE, null, getScope().getCFG().getDescriptor().getName(),
				parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);

		return false;
	}

	@Override
	public boolean visit(
			ContinueStatement node) {
		JavaContinue cnt = new JavaContinue(getScope().getCFG(), getSourceCodeLocation(node));

		// TODO: labels
		getScope().getControlFlowTracker().addModifier(cnt);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(cnt);
		this.block = new ParsedBlock(cnt, adj, cnt);

		return false;
	}

	@Override
	public boolean visit(
			DoStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new DoStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			EmptyStatement node) {

		NoOp noop = new NoOp(getScope().getCFG(), getSourceCodeLocation(node));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(noop);
		this.block = new ParsedBlock(noop, adj, noop);

		return false;
	}

	@Override
	public boolean visit(
			EnhancedForStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new EnhancedForStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			ExpressionStatement node) {
		Expression expr = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(expr);
		this.block = new ParsedBlock(expr, adj, expr);
		return false;
	}

	@Override
	public boolean visit(
			ForStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new ForStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			IfStatement node) {
		NodeList<CFG, Statement, Edge> nodeList = new NodeList<>(new SequentialEdge());

		getScope().getTracker().enterScope();
		Expression condition = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		nodeList.addNode(condition);

		ParsedBlock trueBlock = getParserContext().evaluate(
				node.getThenStatement(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		nodeList.mergeWith(trueBlock.getBody());

		nodeList.addEdge(new TrueEdge(condition, trueBlock.getBegin()));

		Statement noop = new NoOp(getScope().getCFG(), condition.getLocation());

		getScope().getTracker().exitScope(noop);

		if (trueBlock.canBeContinued()) {
			nodeList.addNode(noop);
			nodeList.addEdge(new SequentialEdge(trueBlock.getEnd(), noop));
		}

		ParsedBlock falseBlock = null;

		getScope().getTracker().enterScope();

		if (node.getElseStatement() != null) {
			falseBlock = getParserContext().evaluate(
					node.getElseStatement(),
					() -> new StatementASTVisitor(getEnvironment(), getScope()));
			if (node.getElseStatement() != null) {

				nodeList.mergeWith(falseBlock.getBody());

				nodeList.addEdge(new FalseEdge(condition, falseBlock.getBegin()));
				if (falseBlock.canBeContinued()) {
					nodeList.addNode(noop);
					nodeList.addEdge(new SequentialEdge(falseBlock.getEnd(), noop));
				}
			}
		} else {
			nodeList.addNode(noop);
			nodeList.addEdge(new FalseEdge(condition, noop));
		}

		getScope().getTracker().exitScope(noop);

		Statement follower = null;
		Statement lastBlockStatement = null;

		if (trueBlock == null || (trueBlock != null && trueBlock.canBeContinued())
				|| falseBlock == null || (falseBlock != null && falseBlock.canBeContinued())) {
			nodeList.addNode(noop);
			follower = noop;
			lastBlockStatement = noop;
		}

		getScope().getCFG().getDescriptor()
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
		Expression e = node.getExpression() != null
				? getParserContext().evaluate(node.getExpression(),
						() -> new ExpressionVisitor(getEnvironment(), getScope()))
				: null;
		Statement ret;
		if (e == null) {
			ret = new Ret(getScope().getCFG(), getSourceCodeLocation(node));
		} else {
			ret = new Return(getScope().getCFG(), getSourceCodeLocation(node), e);
		}

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(ret);
		this.block = new ParsedBlock(ret, adj, ret);

		return false;
	}

	@Override
	public boolean visit(
			SuperConstructorInvocation node) {
		Unit unit = getScope().getCFG().getDescriptor().getUnit();
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

		Expression thisExpression = new VariableRef(getScope().getCFG(), getSourceCodeLocation(node), "this",
				getParserContext().getVariableStaticTypeFromUnitAndGlobals(getScope().getCFG(), "this"));
		List<Expression> parameters = new ArrayList<>();
		parameters.add(thisExpression);

		if (!node.arguments().isEmpty()) {
			for (Object args : node.arguments()) {
				ASTNode e = (ASTNode) args;
				Expression expr = getParserContext().evaluate(e,
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
				parameters.add(expr);
			}
		}

		JavaUnresolvedCall call = new JavaUnresolvedCall(getScope().getCFG(),
				getSourceCodeLocationManager(node, true).nextColumn(),
				Call.CallType.INSTANCE, superclassName, simpleName, parameters.toArray(new Expression[0]));
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(call);
		this.block = new ParsedBlock(call, adj, call);

		return false;
	}

	@Override
	public boolean visit(
			SwitchStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new SwitchStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			SynchronizedStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		Expression syncTarget = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));

		SyntheticCodeLocationManager syntheticLocMan = getParserContext()
				.getCurrentSyntheticCodeLocationManager(getSource());
		Statement syntheticCondition = new NotEqual(getScope().getCFG(), syntheticLocMan.nextLocation(), syncTarget,
				new JavaNullLiteral(getScope().getCFG(), syntheticLocMan.nextLocation()));

		adj.addNode(syntheticCondition);

		ParsedBlock synchronizedBody = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		adj.mergeWith(synchronizedBody.getBody());

		adj.addEdge(new TrueEdge(syntheticCondition, synchronizedBody.getBegin()));

		Statement noop = new NoOp(getScope().getCFG(), syntheticLocMan.nextLocation());

		if (synchronizedBody.canBeContinued()) {
			adj.addNode(noop);
			adj.addEdge(new SequentialEdge(synchronizedBody.getEnd(), noop));
		}

		JavaClassType npeType = JavaClassType.lookup("java.lang.NullPointerException");
		Statement nullPointerTrigger = new JavaThrow(getScope().getCFG(), syntheticLocMan.nextLocation(),
				new JavaNewObj(getScope().getCFG(), syntheticLocMan.nextLocation(),
						new JavaReferenceType(npeType),
						new JavaStringLiteral(getScope().getCFG(),
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

		getScope().getCFG().getDescriptor()
				.addControlFlowStructure(new SynchronizedBlock(adj, syncTarget, syntheticCondition,
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
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		Type type = getParserContext().evaluate(node.getType(), () -> visitor);
		if (type.isInMemoryType()) {
			type = new JavaReferenceType(type);
		}
		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String variableName = fragment.getName().getIdentifier();
			type = visitor.liftToArray(type, fragment);

			VariableRef ref = new VariableRef(getScope().getCFG(),
					getSourceCodeLocation(fragment),
					variableName, type);
			Expression initializer;

			JavaAssignment assignment;

			if (fragment.getInitializer() == null) {
				initializer = type.defaultValue(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
				assignment = new JavaAssignment(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(), ref,
						initializer);
			} else {
				SourceCodeLocationManager loc = getSourceCodeLocationManager(fragment.getName(), true);
				org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
				initializer = getParserContext().evaluate(expr,
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
				if (initializer == null) {
					initializer = new NullLiteral(getScope().getCFG(),
							getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
					assignment = new JavaAssignment(getScope().getCFG(),
							getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(), ref,
							initializer);
				} else if (initializer instanceof JavaNewArrayWithInitializer) {
					assignment = new JavaAssignment(getScope().getCFG(), loc.getCurrentLocation(), ref,
							((JavaNewArrayWithInitializer) initializer).withStaticType(type));
				} else if (initializer.getStaticType().canBeAssignedTo(type)
						&& !type.equals(initializer.getStaticType())) {
					// implicit cast
					JavaCastExpression cast = new JavaCastExpression(getScope().getCFG(),
							getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation(),
							initializer,
							type);
					assignment = new JavaAssignment(getScope().getCFG(), loc.getCurrentLocation(), ref, cast);
				} else
					assignment = new JavaAssignment(getScope().getCFG(), loc.getCurrentLocation(), ref, initializer);
			}

			adj.addNode(assignment);
			if (getFirst() == null) {
				first = assignment;
			} else {
				adj.addEdge(new SequentialEdge(last, assignment));
			}

			if (getScope().getTracker().hasVariable(variableName))
				throw new ParsingException("variable-declaration", ParsingException.Type.VARIABLE_ALREADY_DECLARED,
						"Variable " + variableName + " already exists in the cfg", getSourceCodeLocation(node));

			getScope().getTracker().addVariable(variableName, assignment, ref.getAnnotations());
			getParserContext().addVariableType(getScope().getCFG(),
					new VariableInfo(variableName, getScope().getTracker().getLocalVariable(variableName)), type);

			last = assignment;
		}

		this.block = new ParsedBlock(first, adj, last);
		return false;
	}

	@Override
	public boolean visit(
			WhileStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new WhileStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

	@Override
	public boolean visit(
			ThrowStatement node) {
		Expression expr = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Throw th = new JavaThrow(getScope().getCFG(), getSourceCodeLocation(node), expr);

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(th);
		this.block = new ParsedBlock(th, adj, th);
		return false;
	}

	@Override
	public boolean visit(
			TryStatement node) {
		this.block = getParserContext().evaluate(node,
				() -> new TryStatementASTVisitor(getEnvironment(), getScope()));
		return false;
	}

}
