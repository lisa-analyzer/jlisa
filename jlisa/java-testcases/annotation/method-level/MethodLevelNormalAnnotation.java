import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodLevelNormalAnnotation {
    char value();
    short value2();
    boolean value3();
}
