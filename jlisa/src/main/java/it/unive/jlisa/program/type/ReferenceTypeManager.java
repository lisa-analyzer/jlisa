package it.unive.jlisa.program.type;


import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

import java.util.HashMap;
import java.util.Map;

import it.unive.jlisa.types.JavaClassType;

public class ReferenceTypeManager {
    private static final Map<JavaClassType, ReferenceType> map = new HashMap<JavaClassType, ReferenceType>();


    public static ReferenceType get(JavaClassType classType) {
        ReferenceType refType;
        if ((refType = map.get(classType)) == null) {
            refType = new ReferenceType(classType);
            map.put(classType, refType);
        }
        return refType;
    };

    public static ReferenceType get(Type type) throws Exception {
        ReferenceType refType;
        if (type instanceof JavaClassType classType) {

            if ((refType = map.get(classType)) == null) {
                refType = new ReferenceType(classType);
                map.put(classType, refType);
                return refType;
            }
            return refType;
        }
        throw new Exception("Type " + type + " is not a JavaClassType.");
    }

    public static boolean isReferencingJavaStringObject(ReferenceType refType) {
        return true;
    }

    public static boolean isReferencingJavaStringObject(Type type) {
        if (type instanceof ReferenceType refType) {
            return isReferencingJavaStringObject(refType);
        }
        return false;
    }
}
