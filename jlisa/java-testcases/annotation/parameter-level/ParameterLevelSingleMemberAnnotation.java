package it.unive.jlisa;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterLevelSingleMemberAnnotation {
    String value();
}
