import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithQualifiedNameMember {
    String value();
    int value2();
    boolean value3();
}