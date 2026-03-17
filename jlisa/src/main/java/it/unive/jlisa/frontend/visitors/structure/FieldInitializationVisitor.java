package it.unive.jlisa.frontend.visitors.structure;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.ResultHolder;
import it.unive.jlisa.frontend.visitors.ScopedVisitor;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.expression.TypeASTVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldInitializationVisitor extends ScopedVisitor<MethodScope>
		implements
		ResultHolder<NodeList<CFG, Statement, Edge>> {
	private it.unive.lisa.program.cfg.statement.Statement first;
	private it.unive.lisa.program.cfg.statement.Statement last;
	private NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

	public FieldInitializationVisitor(
			ParsingEnvironment environment,
			MethodScope scope) {
		super(environment, scope);
	}

	public boolean visit(
			FieldDeclaration node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(getEnvironment(), getScope().getParentScope().getUnitScope());
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);
		SyntheticCodeLocationManager locationManager = getParserContext()
				.getCurrentSyntheticCodeLocationManager(getSource());

		VariableRef thisExpr = new VariableRef(getScope().getCFG(), locationManager.nextLocation(), "this");

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String identifier = fragment.getName().getIdentifier();
			type = typeVisitor.liftToArray(type, fragment);

			it.unive.lisa.program.cfg.statement.Expression initializer = null;
			if (fragment.getInitializer() != null) {
				it.unive.lisa.program.cfg.statement.Expression candidate = getParserContext().evaluate(
						fragment.getInitializer(),
						() -> new ExpressionVisitor(getEnvironment(), getScope()));
				if (candidate != null) {
					initializer = candidate;
				}
			} else {
				initializer = type.defaultValue(getScope().getCFG(), locationManager.nextLocation());
			}

			JavaAssignment assignment = new JavaAssignment(getScope().getCFG(), locationManager.nextLocation(),
					new AccessInstanceGlobal(getScope().getCFG(), locationManager.nextLocation(), thisExpr, identifier),
					initializer);
			block.addNode(assignment);
			if (first == null) {
				first = assignment;
			}
			if (last != null) {
				block.addEdge(new SequentialEdge(last, assignment));
			}
			last = assignment;
		}
		return false;
	}

	public NodeList<CFG, Statement, Edge> getBlock() {
		return block;
	}

	@Override
	public NodeList<CFG, Statement, Edge> getResult() {
		return block;
	}

	public Statement getFirst() {
		return this.first;
	}

	public Statement getLast() {
		return this.last;
	}
}
