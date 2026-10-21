import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassLevelInfixExpressionNormalAnnotation {
    String value();
    String value2();
}
