package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassASTVisitor extends JavaASTVisitor{

    public ClassASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ClassUnit cUnit = (ClassUnit) getProgram().getUnit(node.getName().toString());

        if (node.getSuperclassType() != null) {
            TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
            node.getSuperclassType().accept(visitor);
            it.unive.lisa.type.Type superType = visitor.getType();
            if (superType != null) {
                it.unive.lisa.program.Unit superUnit = getProgram().getUnit(superType.toString());
                if (superUnit instanceof it.unive.lisa.program.CompilationUnit) {
                    cUnit.addAncestor((it.unive.lisa.program.CompilationUnit)superUnit);
                }
            }
        } else {
            cUnit.addAncestor(JavaClassType.lookup("Object", null).getUnit());
        }
        if (!node.permittedTypes().isEmpty()) {
            parserContext.addException(new ParsingException("permits", ParsingException.Type.UNSUPPORTED_STATEMENT, "Permits is not supported.", getSourceCodeLocation(node)));
        }
        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        it.unive.lisa.program.cfg.statement.Statement first = null;
        it.unive.lisa.program.cfg.statement.Statement last = null;

        for (FieldDeclaration fd : node.getFields()) {
            FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
            fd.accept(visitor);
        }
        for (MethodDeclaration md : node.getMethods()) {
            MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
            md.accept(visitor);
            if (md.isConstructor()) {
                fixConstructorCFG(visitor.getCFG(), node.getFields());
            }
        }

        return false;
    }

    public void fixConstructorCFG(CFG cfg, FieldDeclaration[] fields) {
        Statement entryPoint = cfg.getEntrypoints().iterator().next();
        Statement injectionPoint = entryPoint;

        if (injectionPoint instanceof UnresolvedCall call) {
            if ("super".equals(call.getConstructName())) {
                List<Edge> outEdges = new ArrayList<>(cfg.getNodeList().getOutgoingEdges(injectionPoint));
                if (outEdges.size() == 1) {
                    injectionPoint = outEdges.getFirst().getDestination();
                }
            }
        }

        if (injectionPoint instanceof UnresolvedCall &&
                ((UnresolvedCall) injectionPoint).getConstructName().equals(cfg.getDescriptor().getName())) {
            return;
        }

        Statement first = null, last = null;

        for (FieldDeclaration field : fields) {
            FieldInitializationVisitor initVisitor = new FieldInitializationVisitor(parserContext, source, compilationUnit, cfg);
            field.accept(initVisitor);

            if (initVisitor.getBlock() != null) {
                cfg.getNodeList().mergeWith(initVisitor.getBlock());

                if (first == null) {
                    first = initVisitor.getFirst();
                } else {
                    cfg.addEdge(new SequentialEdge(last, initVisitor.getFirst()));
                }
                last = initVisitor.getLast();
            }
        }

        if (first != null) {
            if (injectionPoint.equals(entryPoint)) {
                cfg.getEntrypoints().clear();
                cfg.getEntrypoints().add(first);
                cfg.addEdge(new SequentialEdge(last, entryPoint));
            } else {
                for (Edge edge : cfg.getIngoingEdges(injectionPoint)) {
                    cfg.getEdges().remove(edge);
                    cfg.addEdge(new SequentialEdge(edge.getSource(), first));
                }
                cfg.addEdge(new SequentialEdge(last, injectionPoint));
            }
        }
    }
}