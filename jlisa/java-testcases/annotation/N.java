
public class N {

    public N(
            @ParameterLevelSingleMemberAnnotation("paramVal") String param1,
            @ParameterLevelNormalAnnotation(value=10, value2="b", value3=false, value4='s') String param2,
            String param3){
    }
}
