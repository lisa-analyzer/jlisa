import java.util.jar.JarFile;
import java.util.logging.Logger;

@ClassLevelInfixExpressionNormalAnnotation(value = "ABC" + "123", value2 = "DEF" + Constants.STRING_CONSTANT)
public class S6 extends DerivedBaseClass implements BaseInterface {

    private static final String LOCAL_STRING_CONSTANT = "def";
    private static final String ANOTHER_LOCAL_STRING_CONSTANT = LOCAL_STRING_CONSTANT + "123";
    private static final String ALIAS_STRING_CONSTANT = Constants.STRING_CONSTANT;

    @MethodLevelInfixExpressionSingleAnnotation("ABC" + "DEF")
    public void s6_1() {
    }

    @MethodLevelInfixExpressionSingleAnnotation("ABC" + LOCAL_STRING_CONSTANT)
    public void s6_2() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(ANOTHER_LOCAL_STRING_CONSTANT + "ABC")
    public void s6_3() {
    }

    @MethodLevelInfixExpressionSingleAnnotation("ABC" + Constants.STRING_CONSTANT)
    public void s6_4() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(Constants.COMPOSED_STRING_CONSTANT + "123")
    public void s6_5() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(
            ANOTHER_LOCAL_STRING_CONSTANT + "ABC"
                    + Constants.STRING_CONSTANT + Constants.COMPOSED_STRING_CONSTANT)
    public void s6_6() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(Logger.GLOBAL_LOGGER_NAME + "123")
    public void s6_7() {
    }

    @MethodLevelInfixExpressionSingleAnnotationWithArray({"ABC" + "DEF", Constants.STRING_CONSTANT + LOCAL_STRING_CONSTANT})
    public void s6_8() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(BASECLASS_CONSTANT + INTERFACE_CONSTANT)
    public void s6_9() {
    }

    @MethodLevelInfixExpressionSingleAnnotation("ABC" + JarFile.MANIFEST_NAME)
    public void s6_10() {
    }

    @MethodLevelInfixExpressionSingleAnnotation(ALIAS_STRING_CONSTANT + "-alias")
    public void s6_11() {
    }

    @MethodLevelInfixExpressionSingleAnnotation("DEF" + 4)
    public void s6_12() {
    }

    public static class Nested extends Constants {

        @MethodLevelInfixExpressionSingleAnnotation(STRING_CONSTANT + "-nested")
        public void nested() {
        }
    }
}
