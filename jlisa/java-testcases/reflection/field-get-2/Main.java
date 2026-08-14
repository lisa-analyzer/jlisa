import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("C");
		Class c = Class.forName(s);

		Field someField = c.getField("someField");
		assert(someField.getName().equals("someField"));
		assert(someField.getType() == boolean.class);

		Field f1 = c.getField("field1");
		assert(f1.getName().equals("field1"));
		assert(f1.getType() == int.class);

		Field f2 = c.getField("field2");
		assert(f2.getName().equals("field2"));
		assert(f2.getType() == String.class);

		Field f3 = c.getField("field3");
		assert(f3.getName().equals("field3"));
		assert(f3.getType() == double.class);

		return;
	}

	private static Object getFieldValue(Object o, Field f) {
		return f.get(o);
	}
}

interface A {
	public static int field1 = 42;
	public static String field2 = "hello";
}

interface B {
	public static double field3 = 5.0;
}

class C implements A, B {
	public boolean someField = true;
}
