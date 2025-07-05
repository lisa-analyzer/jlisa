package it.unive.jlisa.program;

import it.unive.jlisa.program.java.constructs.object.ObjectConstructor;
import it.unive.jlisa.program.java.constructs.string.constructors.StringCopyConstructor;
import it.unive.jlisa.program.java.constructs.string.constructors.StringEmptyConstructor;
import it.unive.jlisa.program.java.constructs.string.constructors.StringLiteralConstructor;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.type.ReferenceType;
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
        JavaClassType stringClassType = JavaClassType.lookup("String", str);
        str.addAncestor(rootClassUnit);
        str.addInstanceGlobal(new Global(unknownLocation, str, "value", true));
        str.addInstanceGlobal(new Global(unknownLocation, str, "length", true));
        str.addInstanceCodeMember(new StringEmptyConstructor(unknownLocation, str, new ReferenceType(stringClassType)));
        str.addInstanceCodeMember(new StringLiteralConstructor(unknownLocation, str, new ReferenceType(stringClassType)));
        str.addInstanceCodeMember(new StringCopyConstructor(unknownLocation, str, new ReferenceType(stringClassType)));
        //str.addInstanceCodeMember(new StringContains(unknownLocation, str));
        //str.addInstanceCodeMember(new StringEndsWith(unknownLocation, str));
        //str.addInstanceCodeMember(new StringEquals(unknownLocation, str));
        //str.addInstanceCodeMember(new StringIndexOf(unknownLocation, str));
        //str.addInstanceCodeMember(new StringLength(unknownLocation, str));
        //str.addInstanceCodeMember(new StringReplace(unknownLocation, str));
        //str.addInstanceCodeMember(new StringStartsWith(unknownLocation, str));
        //str.addInstanceCodeMember(new StringSubstring(unknownLocation, str));


        return str;
    }

}
