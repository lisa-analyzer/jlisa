package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.jlisa.types.JavaArrayType;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.type.ArrayType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import org.eclipse.jdt.core.dom.*;

import javax.lang.model.type.TypeVisitor;
import java.util.ArrayList;
import java.util.List;

public class FieldDeclarationVisitor extends JavaASTVisitor {
    it.unive.lisa.program.CompilationUnit lisacompilationUnit;

    public FieldDeclarationVisitor(ParserContext parserContext, String source, it.unive.lisa.program.CompilationUnit lisacompilationUnit, CompilationUnit astCompilationUnit) {
        super(parserContext, source, astCompilationUnit);
        this.lisacompilationUnit = lisacompilationUnit;
    }
    @Override
    public boolean visit(FieldDeclaration node) {
        int modifiers = node.getModifiers();
        TypeASTVisitor typeVisitor = new TypeASTVisitor(parserContext, source, compilationUnit);
        node.getType().accept(typeVisitor);
        Type type = typeVisitor.getType();


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

            String identifier = fragment.getName().getIdentifier();

            boolean isStatic = Modifier.isStatic(modifiers);
            Global global = new Global(getSourceCodeLocation(node), lisacompilationUnit, identifier, !isStatic, type, new Annotations());
            if (isStatic) {
                lisacompilationUnit.addGlobal(global);
            } else {
                lisacompilationUnit.addInstanceGlobal(global);
            }
        }

        return false;
    }
}
