package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.jlisa.types.JavaArrayType;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldInitializationVisitor extends JavaASTVisitor{
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Statement first;
    private it.unive.lisa.program.cfg.statement.Statement last;
    private NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

    public FieldInitializationVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    public boolean visit(FieldDeclaration node) {
        int modifiers = node.getModifiers();
        TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getType().accept(typeVisitor);
        Type type = typeVisitor.getType();

        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        VariableRef thisExpr = new VariableRef(cfg, unknownLocation, "this");
        for (Object f : node.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
            String variableName = fragment.getName().getIdentifier();
            SourceCodeLocation loc = getSourceCodeLocation(fragment);
            if (fragment.getExtraDimensions() != 0) {
                if (type instanceof ArrayType) {
                    ArrayType arrayType = (ArrayType) type;
                    int dim = arrayType.getDimensions();
                    type = JavaArrayType.lookup(arrayType.getBaseType(), dim + fragment.getExtraDimensions());
                } else {
                    type = JavaArrayType.lookup(type, fragment.getExtraDimensions());
                }
            }
            it.unive.lisa.program.cfg.statement.Expression initializer = null;
            if (fragment.getInitializer() != null) {
                ExpressionVisitor initializerVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
                Expression expression = fragment.getInitializer();
                expression.accept(initializerVisitor);
                if (initializerVisitor.getExpression() != null) {
                    initializer = initializerVisitor.getExpression();
                }
            } else {
                initializer = JavaTypeSystem.getDefaultLiteral(type, cfg, unknownLocation);
            }
            String identifier = fragment.getName().getIdentifier();
            it.unive.lisa.program.cfg.statement.Assignment assignment = new it.unive.lisa.program.cfg.statement.Assignment(cfg, unknownLocation, new AccessInstanceGlobal(cfg, unknownLocation, thisExpr, identifier), initializer);
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
