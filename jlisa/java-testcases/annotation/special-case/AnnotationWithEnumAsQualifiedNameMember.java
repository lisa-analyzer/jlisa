import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithEnumAsQualifiedNameMember {
    Enumeration value();
    Enumeration value2();
}