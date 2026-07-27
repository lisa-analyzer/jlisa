import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldLevelNormalAnnotation {
    long value();
    String value2();
    char value3();
}
