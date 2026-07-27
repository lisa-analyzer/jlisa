
public class L {

    public void L_1(String param) {
    }

    public void L_2(
            @ParameterLevelNormalAnnotation(
                    value = 1,
                    value2 = "paramVal",
                    value3 = false,
                    value4 = 'p') String param1,
            @ParameterLevelNormalAnnotation(
                    value = 2,
                    value2 = "paramVal2",
                    value3 = true,
                    value4 = 'a') Long param2,
            Integer param3) {
    }
}
