import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("Holder");

		String fieldName;
		if (args.length == 0)
			fieldName = "staticCount";
		else
			fieldName = "staticName";

		Field f1 = c.getField(fieldName);
		Object val1 = f1.get(null);

		Field f2 = c.getField("staticPi");
		Object val2 = f2.get(null);

		Field f3 = c.getField("staticWrapperCount");
		Object val3 = f3.get(null);
	}
}

class Holder {
	public static int staticCount = 42;
	public static double staticPi = 3.14;
	public static Integer staticWrapperCount = 100;
	public static String staticName = "holder";
}