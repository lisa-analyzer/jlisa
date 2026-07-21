package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.cfg.controlflow.switches.DefaultSwitchCase;
import it.unive.jlisa.program.cfg.controlflow.switches.Switch;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchDefault;
import it.unive.jlisa.program.cfg.controlflow.switches.instrumentations.SwitchEqualityCheck;
import it.unive.jlisa.program.cfg.statement.controlflow.JavaBreak;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.lisa.program.Global;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;

class SwitchStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private ParsedBlock block;
	private Expression switchExpression;

	SwitchStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			SwitchStatement node) {
		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());

		switchExpression = getParserContext().evaluate(node.getExpression(),
				() -> new ExpressionVisitor(getEnvironment(), getScope()));
		Statement noop = new NoOp(getScope().getCFG(),
				getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());

		boolean usedNoop = false;

		List<it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase> cases = new ArrayList<>();

		DefaultSwitchCase defaultCase = null;
		SwitchDefault switchDefault = null;

		List<SwitchEqualityCheck> workList = new ArrayList<>();

		List<Statement> caseInstrs = new ArrayList<>();
		Statement lastCaseInstr = null;

		getScope().getTracker().enterScope();

		Statement first = null, last = null;
		for (Object o : node.statements()) {
			ParsedBlock caseBlock;
			if (o instanceof SwitchCase) {
				caseBlock = visitSwitchCase((SwitchCase) o);
			} else {
				caseBlock = getParserContext().evaluate(
						(org.eclipse.jdt.core.dom.Statement) o,
						() -> new StatementASTVisitor(getEnvironment(), getScope()));
			}
			boolean isEmptyBlock = caseBlock == null || caseBlock.getBody().getNodes().isEmpty();
			NoOp emptyBlock = null;

			if (isEmptyBlock) {
				emptyBlock = new NoOp(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
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

					for (Statement stBlock : caseBlock.getBody()) {
						if (stBlock instanceof JavaBreak)
							for (Statement stInstr : caseInstrs) {
								if (stInstr.equals(stBlock)) {
									adj.addNode(noop);
									usedNoop = true;
									adj.addEdge(new SequentialEdge(stInstr, noop));
								}
							}
					}

				}

				if (switchDefault != null) {
					defaultCase = new DefaultSwitchCase(switchDefault, caseInstrs);
					Statement follower = getFirstInstructionAfterSwitchInstr(switchDefault, caseInstrs);
					if (follower != null) {
						adj.addEdge(new SequentialEdge(switchDefault, follower));
					} else {
						emptyBlock = new NoOp(getScope().getCFG(),
								getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
						adj.addNode(emptyBlock);
						adj.addNode(noop);
						usedNoop = true;
						adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
						adj.addEdge(new SequentialEdge(emptyBlock, noop));
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
				if (!lastCaseInstr.stopsExecution() && !lastCaseInstr.throwsError()) {
					adj.addNode(noop);
					usedNoop = true;
					adj.addEdge(new SequentialEdge(lastCaseInstr, noop));
				} else if (follower instanceof NoOp) {
					adj.addNode(noop);
					usedNoop = true;
					adj.addEdge(new SequentialEdge(follower, noop));
				}
			} else {
				emptyBlock = new NoOp(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
				adj.addNode(emptyBlock);
				adj.addEdge(new SequentialEdge(switchDefault, emptyBlock));
				adj.addNode(noop);
				usedNoop = true;
				adj.addEdge(new SequentialEdge(emptyBlock, noop));
			}
		}

		for (SwitchEqualityCheck switchCondition : workList) {

			if (caseInstrs.size() > 1) {
				Statement next = getFirstInstructionAfterSwitchInstr(switchCondition, caseInstrs);
				if (next == null && usedNoop)
					next = noop;
				adj.addEdge(new TrueEdge(switchCondition, next));
			} else {
				emptyBlock = new NoOp(getScope().getCFG(),
						getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
				adj.addNode(emptyBlock);
				adj.addNode(noop);
				usedNoop = true;
				adj.addEdge(new TrueEdge(defaultCase.getEntry(), emptyBlock));
				adj.addEdge(new SequentialEdge(emptyBlock, noop));
			}

			cases.add(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase(switchCondition, caseInstrs));
		}

		for (int i = 0; i < cases.size() - 1; i++)
			adj.addEdge(new FalseEdge(cases.get(i).getCondition(), cases.get(i + 1).getCondition()));

		if (defaultCase != null && !cases.isEmpty())
			adj.addEdge(new FalseEdge(cases.getLast().getCondition(), defaultCase.getEntry()));

		lazySwitchEdgeBindingCleanUp(adj, cases, defaultCase != null ? defaultCase.getEntry() : null);

		if (node.statements().isEmpty() || (cases.isEmpty() && defaultCase == null)) {
			emptyBlock = new NoOp(getScope().getCFG(),
					getParserContext().getCurrentSyntheticCodeLocationManager(getSource()).nextLocation());
			adj.addNode(emptyBlock);
			adj.addNode(noop);
			usedNoop = true;
			adj.addEdge(new SequentialEdge(emptyBlock, noop));
			first = emptyBlock;
		}

		getScope().getTracker().exitScope(noop);

		// TODO: labels
		getScope().getCFG().getDescriptor()
				.addControlFlowStructure(new Switch(adj,
						!cases.isEmpty() ? cases.getFirst().getCondition()
								: defaultCase != null ? defaultCase.getEntry() : emptyBlock,
						usedNoop ? noop : null,
						cases.toArray(new it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase[cases.size()]),
						defaultCase));

		if (usedNoop)
			getScope().getControlFlowTracker().endControlFlowOf(adj,
					!cases.isEmpty() ? cases.getFirst().getCondition()
							: defaultCase != null ? defaultCase.getEntry() : emptyBlock,
					noop, noop, null);
		this.block = new ParsedBlock(first, adj, usedNoop ? noop : null);
		return false;
	}

	private ParsedBlock visitSwitchCase(
			SwitchCase node) {
		Expression expr;
		if (switchExpression.getStaticType().isReferenceType()
				&& switchExpression.getStaticType().asReferenceType().getInnerType().isUnitType()
				&& switchExpression.getStaticType().asReferenceType().getInnerType().asUnitType()
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
			EnumUnit switchType = (EnumUnit) switchExpression.getStaticType().asReferenceType().getInnerType()
					.asUnitType().getUnit();
			Global global = switchType.getGlobal(name.getIdentifier());
			if (global == null)
				throw new ParsingException("switch-case",
						ParsingException.Type.UNSUPPORTED_STATEMENT,
						"Enum switch case " + name.getIdentifier() + " not found in enum " + switchType.getName(),
						getSourceCodeLocation(node));
			expr = new JavaAccessGlobal(getScope().getCFG(), getSourceCodeLocation(node), switchType, global);
		} else {
			expr = getParserContext().evaluate(node, () -> new ExpressionVisitor(getEnvironment(), getScope()));
		}

		SourceCodeLocationManager mgr = getSourceCodeLocationManager(node);
		Statement st = expr != null
				? new SwitchEqualityCheck(getScope().getCFG(), mgr.nextColumn(), switchExpression, expr)
				: new SwitchDefault(getScope().getCFG(), mgr.nextColumn());

		NodeList<CFG, Statement, Edge> adj = new NodeList<>(new SequentialEdge());
		adj.addNode(st);
		return new ParsedBlock(st, adj, st);
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

				for (Statement st : c.getBody()) {
					if (st.stopsExecution() && e.getSource().equals(st)) {
						adj.removeEdge(e);
					}
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
	public ParsedBlock getResult() {
		return block;
	}
}
