public class S7 {

    @MethodLevelNormalAnnotation(value = 'a', value2 = 2 + 3, value3 = false)
    // Infix expression with non-string values are not supported; must throw exception
    public void s1() {
    }
}
