import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodLevelInfixExpressionSingleAnnotationWithArray {
    String[] value();
}
