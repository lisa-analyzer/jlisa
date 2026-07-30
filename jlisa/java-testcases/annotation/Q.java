
public class Q {

    @FieldLevelNormalAnnotation(value=1000L, value2="abc", value3='x')
    static public String field1;

    @FieldLevelNormalAnnotation(value=2000L, value2="dfg", value3='y')
    final private int field2 = 0;

    @FieldLevelNormalAnnotation(value=3000L, value2="hji", value3='z')
    protected char field3;
}
