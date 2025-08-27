package it.unive.jlisa.program.cfg;

import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.type.Type;

public class JavaCodeMemberDescriptor extends CodeMemberDescriptor {
    public JavaCodeMemberDescriptor(CodeLocation location, Unit unit, boolean instance, String name, Parameter... formals) {
        super(location, unit, instance, name, formals);
    }

    public JavaCodeMemberDescriptor(CodeLocation location, Unit unit, boolean instance, String name, Type returnType, Parameter... formals) {
        super(location, unit, instance, name, returnType, formals);
    }

    public JavaCodeMemberDescriptor(CodeLocation location, Unit unit, boolean instance, String name, Type returnType, Annotations annotations, Parameter... formals) {
        super(location, unit, instance, name, returnType, annotations, formals);
    }

    public boolean matchesSignature(
            CodeMemberDescriptor reference) {
        if (!getName().equals(reference.getName()))
            return false;

        if (getFormals().length != reference.getFormals().length)
            return false;

        for (int i = 0; i < getFormals().length; i++) {
            if (!getFormals()[i].getStaticType().equals(reference.getFormals()[i].getStaticType())) {
                    return false;
                }
            }
        return true;
    }
}
