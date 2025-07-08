package it.unive.jlisa.program.unit;

import it.unive.jlisa.program.java.constructs.string.StringConcat;
import it.unive.jlisa.program.java.constructs.string.StringLength;
import it.unive.jlisa.program.java.constructs.string.constructors.StringCopyConstructor;
import it.unive.jlisa.program.java.constructs.string.constructors.StringEmptyConstructor;
import it.unive.jlisa.program.java.constructs.string.constructors.StringLiteralConstructor;
import it.unive.jlisa.program.location.JavaRuntimeLocation;
import it.unive.jlisa.program.type.StringReferenceType;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;

public class StringClassUnit extends ClassUnit {

    public StringClassUnit(Program program) {
        super(JavaRuntimeLocation.INSTANCE, program, "String", true);
        JavaClassType.lookup("String", this);
        this.addAncestor(JavaClassType.getObjectClassUnit());
        this.addInstanceGlobal(new Global(JavaRuntimeLocation.INSTANCE, this, "value", true));
        this.addInstanceGlobal(new Global(JavaRuntimeLocation.INSTANCE, this, "length", true));
        this.addInstanceCodeMember(new StringEmptyConstructor(JavaRuntimeLocation.INSTANCE, this, StringReferenceType.get()));
        this.addInstanceCodeMember(new StringLiteralConstructor(JavaRuntimeLocation.INSTANCE, this, StringReferenceType.get()));
        this.addInstanceCodeMember(new StringCopyConstructor(JavaRuntimeLocation.INSTANCE, this, StringReferenceType.get()));
        this.addInstanceCodeMember(new StringLength(JavaRuntimeLocation.INSTANCE, this, StringReferenceType.get()));
        this.addInstanceCodeMember(new StringConcat(JavaRuntimeLocation.INSTANCE, this, StringReferenceType.get()));
    }

}
