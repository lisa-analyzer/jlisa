import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassLevelNormalAnnotation {
    int value();
    String value2();
    boolean value3();
}
