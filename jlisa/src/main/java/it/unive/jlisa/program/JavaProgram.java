package it.unive.jlisa.program;

import it.unive.jlisa.program.java.constructs.object.ObjectConstructor;
import it.unive.jlisa.program.java.constructs.string.*;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.type.TypeSystem;

public class JavaProgram extends Program {
    private ClassUnit rootClassUnit;
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
        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        rootClassUnit = new ClassUnit(unknownLocation, this, "Object", false);
        JavaClassType.lookup("Object", rootClassUnit);
        rootClassUnit.addInstanceCodeMember(new ObjectConstructor(unknownLocation, rootClassUnit));
        this.addUnit(rootClassUnit);
        this.addUnit(getStringClassUnit());

    }

    public ClassUnit getStringClassUnit() {
        SourceCodeLocation unknownLocation = new SourceCodeLocation("java-runtime", 0, 0);
        ClassUnit str = new ClassUnit(unknownLocation, this, "String", true);
        str.addAncestor(rootClassUnit);

        str.addInstanceCodeMember(new StringContains(unknownLocation, str));
        str.addInstanceCodeMember(new StringEndsWith(unknownLocation, str));
        str.addInstanceCodeMember(new StringEquals(unknownLocation, str));
        str.addInstanceCodeMember(new StringIndexOf(unknownLocation, str));
//        str.addInstanceCodeMember(new StringLength(unknownLocation, str));
        str.addInstanceCodeMember(new StringReplace(unknownLocation, str));
        str.addInstanceCodeMember(new StringStartsWith(unknownLocation, str));
        str.addInstanceCodeMember(new StringSubstring(unknownLocation, str));

        JavaClassType.lookup("String", str);
        return str;
    }

}
