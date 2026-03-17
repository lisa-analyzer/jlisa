package it.unive.jlisa.frontend.visitors.statement;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

class TryStatementASTVisitor extends ScopedVisitor<MethodScope> implements ResultHolder<ParsedBlock> {

	private ParsedBlock block;

	TryStatementASTVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
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
		// body of the try
		ParsedBlock body = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));
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
			CatchResult result = visitCatchClause(cl);
			CatchBlock catchBlock = result.catchBlock();
			ParsedBlock visit = result.parsedBlock();
			catches.add(catchBlock);
			catchBodies.add(visit);
			trycatch.mergeWith(visit.getBody());

			if (body.canBeContinued())
				trycatch.addEdge(
						new ErrorEdge(body.getEnd(), visit.getBegin(), catchBlock.getIdentifier(), tryBlock,
								catchBlock.getExceptions()));
			for (Statement st : body.getBody().getNodes())
				if (st.stopsExecution())
					trycatch.addEdge(new ErrorEdge(st, visit.getBegin(), catchBlock.getIdentifier(), tryBlock,
							catchBlock.getExceptions()));

			if (visit.canBeContinued())
				// non-stopping last statement
				normalExits.add(visit.getEnd());
		}

		// lastly, we parse the finally block and
		// we connect it with the body (or the else block if it exists) and with
		// each catch block
		ParsedBlock finallyBlock = null;
		if (node.getFinally() != null) {
			finallyBlock = getParserContext().evaluate(
					node.getFinally(),
					() -> new StatementASTVisitor(getEnvironment(), getScope()));
			trycatch.mergeWith(finallyBlock.getBody());
		}

		// this is the noop closing the whole try-catch, only if there is at
		// least one path that does
		// not return/throw anything
		Statement noop = new NoOp(getScope().getCFG(), getSourceCodeLocation(node));
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
		getScope().getCFG().getDescriptor().addProtectionBlock(
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

	private record CatchResult(
			CatchBlock catchBlock,
			ParsedBlock parsedBlock) {
	}

	private CatchResult visitCatchClause(
			CatchClause node) {
		getScope().getTracker().enterScope();

		// type of the exception
		Type type = getParserContext().evaluate(
				node.getException().getType(),
				() -> new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope()));
		type = type.isInMemoryType() ? new JavaReferenceType(type) : type;

		// exception variable
		SingleVariableDeclaration exc = node.getException();
		VariableRef v = new VariableRef(getScope().getCFG(), getSourceCodeLocation(exc), exc.getName().getIdentifier(),
				type);
		getScope().getTracker().addVariable(v.toString(), v, new Annotations());
		getParserContext().addVariableType(getScope().getCFG(),
				new VariableInfo(v.toString(),
						getScope().getTracker() != null ? getScope().getTracker().getLocalVariable(v.toString())
								: null),
				type);

		// exception body / body of the catch clause
		ParsedBlock body = getParserContext().evaluate(
				node.getBody(),
				() -> new StatementASTVisitor(getEnvironment(), getScope()));

		CatchBlock catchBlock = new CatchBlock(
				v,
				new ProtectedBlock(body.getBegin(), body.getEnd(), body.getBody().getNodes()),
				type);

		getScope().getTracker().exitScope(body.getEnd());
		return new CatchResult(catchBlock, body);
	}

	@Override
	public ParsedBlock getResult() {
		return block;
	}
}
