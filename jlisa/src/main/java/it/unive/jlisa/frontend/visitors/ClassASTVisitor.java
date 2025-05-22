package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.NativeCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.type.ReferenceType;
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

        for (FieldDeclaration fd : node.getFields()) {
            FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
            fd.accept(visitor);
        }
        boolean createDefaultConstructor = true;
        for (MethodDeclaration md : node.getMethods()) {
            MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
            md.accept(visitor);
            if (md.isConstructor()) {
                createDefaultConstructor = false;
                fixConstructorCFG(visitor.getCFG(), node.getFields());
            }
        }
        if (createDefaultConstructor) {
            CFG defaultConstructor = createDefaultConstructor(cUnit);
            fixConstructorCFG(defaultConstructor, node.getFields());
        }

        return false;
    }

    private CFG createDefaultConstructor(ClassUnit classUnit) {

        it.unive.lisa.type.Type type = getProgram().getTypes().getType(classUnit.getName());

        List<Parameter> parameters = new ArrayList<>();
        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        parameters.add(new Parameter(unknownLocation, "this", new ReferenceType(type), null, new Annotations()));

        Annotations annotations = new Annotations();
        Parameter[] paramArray = parameters.toArray(new Parameter[0]);
        CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(unknownLocation, classUnit, true, classUnit.getName(), type, annotations, paramArray);
        CFG cfg = new CFG(codeMemberDescriptor);

        String superClassName = classUnit.getImmediateAncestors().iterator().next().getName();

        Statement call = new UnresolvedCall(cfg, unknownLocation, Call.CallType.INSTANCE, null, superClassName, new VariableRef(cfg, unknownLocation, "this"));

        Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
        cfg.addNode(ret);
        cfg.addNode(call);
        cfg.getEntrypoints().add(call);
        cfg.addEdge(new SequentialEdge(call, ret));
        classUnit.addInstanceCodeMember(cfg);
        return cfg;
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