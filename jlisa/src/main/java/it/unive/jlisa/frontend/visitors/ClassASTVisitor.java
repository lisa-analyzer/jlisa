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
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.type.ReferenceType;
import org.eclipse.jdt.core.dom.*;

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
        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        List<Parameter> parameters = new ArrayList<Parameter>();
        it.unive.lisa.type.Type thisType = getProgram().getTypes().getType(cUnit.getName());
        parameters.add(new Parameter(unknownLocation, "this", new ReferenceType(thisType), null, new Annotations()));
        CodeMemberDescriptor descriptor = new CodeMemberDescriptor(unknownLocation, cUnit,true, "$init_attributes", parameters.toArray(new Parameter[parameters.size()]));
        CFG cfg = new CFG(descriptor);
        it.unive.lisa.program.cfg.statement.Statement first = null;
        it.unive.lisa.program.cfg.statement.Statement last = null;

        for (FieldDeclaration fd : node.getFields()) {
            FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
            fd.accept(visitor);
            FieldInitializationVisitor initVisitor = new FieldInitializationVisitor(parserContext, source, compilationUnit, cfg);
            fd.accept(initVisitor);
            if (initVisitor.getBlock() != null) {
                cfg.getNodeList().mergeWith(initVisitor.getBlock());
            }
            if (first == null) {
                first = initVisitor.getFirst();
                cfg.getEntrypoints().add(first);
            }
            if (last != null) {
                cfg.addEdge(new SequentialEdge(last, initVisitor.getFirst()));
            }
            last = initVisitor.getLast();
        }
        Ret ret = new Ret(cfg, unknownLocation);
        cfg.addNode(ret);
        if (last != null) {
            cfg.addEdge(new SequentialEdge(last, ret));
        }
        cUnit.addInstanceCodeMember(cfg);
        for (MethodDeclaration md : node.getMethods()) {
            MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
            md.accept(visitor);
        }



        return false;
    }
}