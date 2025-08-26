package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.VoidType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
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
        SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
        parameters.add(new Parameter(locationManager.nextLocation(), "this", new ReferenceType(type), null, new Annotations()));

        Annotations annotations = new Annotations();
        Parameter[] paramArray = parameters.toArray(new Parameter[0]);
        CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(locationManager.nextLocation(), classUnit, true, classUnit.getName(), VoidType.INSTANCE, annotations, paramArray);
        CFG cfg = new CFG(codeMemberDescriptor);
        parserContext.addVariableType(cfg, "this", new ReferenceType(type));
        String superClassName = classUnit.getImmediateAncestors().iterator().next().getName();

        Statement call = new UnresolvedCall(cfg, locationManager.nextLocation(), Call.CallType.INSTANCE, null, superClassName, new VariableRef(cfg, locationManager.nextLocation(), "this"));

        Ret ret = new Ret(cfg, locationManager.nextLocation());
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
        Unit u = cfg.getDescriptor().getUnit();
        if (!(u instanceof ClassUnit)) {
            throw new RuntimeException("The unit of a constructor must be a class unit");
        }
        ClassUnit classUnit = (ClassUnit) u;
        Unit ancestor = classUnit.getImmediateAncestors().iterator().next();
        boolean implicitlyCallSuper = true;
        if (injectionPoint instanceof UnresolvedCall call) {
            if (ancestor.getName().equals(call.getConstructName())) {
                implicitlyCallSuper = false;
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
        if (implicitlyCallSuper) {
            // add a super() call to this constructor, as a first statement, before the field initializator.
            SyntheticCodeLocationManager locationManager = parserContext.getCurrentSyntheticCodeLocationManager(source);
            Statement call = new UnresolvedCall(cfg, locationManager.nextLocation(), Call.CallType.INSTANCE, null, ancestor.getName(), new VariableRef(cfg, locationManager.nextLocation(), "this"));
            cfg.addNode(call);
            cfg.addEdge(new SequentialEdge(call, injectionPoint));
            cfg.getEntrypoints().clear();
            cfg.getEntrypoints().add(call);
            entryPoint = call;
            //injectionPoint = call;
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
                    cfg.getNodeList().removeEdge(edge);
                    cfg.addEdge(new SequentialEdge(edge.getSource(), first));
                }
                cfg.addEdge(new SequentialEdge(last, injectionPoint));
            }
        }
    }
}