package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import it.unive.jlisa.frontend.ParserContext;
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

public class FieldInitializationVisitor extends BaseCodeElementASTVisitor {
	private CFG cfg;
	private it.unive.lisa.program.cfg.statement.Statement first;
	private it.unive.lisa.program.cfg.statement.Statement last;
	private NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

	public FieldInitializationVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			CFG cfg,
			BaseUnitASTVisitor container) {
		super(parserContext, source, compilationUnit, container);
		this.cfg = cfg;
	}

	public boolean visit(
			FieldDeclaration node) {
		TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit, container);
		node.getType().accept(typeVisitor);
		Type type = typeVisitor.getType();
		if (type.isInMemoryType())
			type = new JavaReferenceType(type);
		SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);

		VariableRef thisExpr = new VariableRef(cfg, locationManager.nextLocation(), "this");

		for (Object f : node.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
			String identifier = fragment.getName().getIdentifier();
			type = typeVisitor.liftToArray(type, fragment);
			
			it.unive.lisa.program.cfg.statement.Expression initializer = null;
			if (fragment.getInitializer() != null) {
				ExpressionVisitor initializerVisitor = new ExpressionVisitor(parserContext, source, compilationUnit,
						cfg, null, container);
				Expression expression = fragment.getInitializer();
				expression.accept(initializerVisitor);
				if (initializerVisitor.getExpression() != null) {
					initializer = initializerVisitor.getExpression();
				}
			} else {
				initializer = type.defaultValue(cfg, locationManager.nextLocation());
			}

			JavaAssignment assignment = new JavaAssignment(cfg, locationManager.nextLocation(),
					new AccessInstanceGlobal(cfg, locationManager.nextLocation(), thisExpr, identifier), initializer);
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

	public Statement getFirst() {
		return this.first;
	}

	public Statement getLast() {
		return this.last;
	}
}
