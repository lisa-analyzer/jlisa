package it.unive.jlisa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterLevelNormalAnnotation {
    int value();
    String value2();
    boolean value3();
    char value4();
}
