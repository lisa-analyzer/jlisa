import java.lang.annotation.*;

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassLevelAnnotationInherited {
    float value1();
    double value2();
    long value3();
    int value4();
    String[] value5();
}
