package it.unive.jlisa.program;

import java.util.ArrayList;
import java.util.List;

import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.type.TypeSystem;

public class JavaProgram extends Program {
    /**
     * Builds an empty program.
     *
     * @param features the language-specific features, algorithms and semantics
     *                 of this program
     * @param types    the type system knowing about the types that appear in
     *                 the program
     */
    public JavaProgram(LanguageFeatures features, TypeSystem types) {
        super(features, types);
        JavaClassType.init(this);
        this.addClassUnits(JavaClassType.getClassUnits());
        this.addInterfaceUnits(new ArrayList<InterfaceUnit>());
    }

    public void addClassUnits(List<ClassUnit> classUnits) {
        classUnits.forEach(this::addUnit);
    }

    public void addInterfaceUnits(List<InterfaceUnit> interfaceUnits) {
        interfaceUnits.forEach(this::addUnit);
    }
    public ClassUnit getClassUnit(String name) throws Exception {
		Unit unit =  getUnit(name);
        if (unit instanceof ClassUnit cUnit) {
            return cUnit;
        }
        throw new Exception("The ClassUnit " + name + " does not exists in the JavaProgram.");
	}

}
