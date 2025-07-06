package it.unive.jlisa.program.unit;

import it.unive.jlisa.program.java.constructs.object.ObjectConstructor;
import it.unive.jlisa.program.location.JavaRuntimeLocation;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.jlisa.types.JavaClassType;

public class ObjectClassUnit extends ClassUnit {

    public ObjectClassUnit(Program program) {
        super(JavaRuntimeLocation.INSTANCE, program, "Object", false);
        JavaClassType.lookup("Object", this);
        this.addInstanceCodeMember(new ObjectConstructor(JavaRuntimeLocation.INSTANCE, this));
    }

}
