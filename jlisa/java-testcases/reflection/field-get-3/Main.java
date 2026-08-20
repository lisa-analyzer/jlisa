import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {

		B a = new B();

		assert(a.name.equals("ziggy"));
		assert(a.z == 12);

		try {
			Object o = B.getSomeField(a, "name");
			assert(o instanceof String);
			String str = (String)o;
			assert(str.equals("ziggy"));
		}
		catch (Exception e) {
			assert false;
		}


		try {
			Object o = B.getSomeField(a, "z");
			assert(o instanceof Double);
			Double d = (Double)o;
			assert(d.doubleValue() == 12);
		}
		catch (Exception e) {
			assert false;
		}

	}
}

class A {
	public String name = "ziggy";
}

class B extends A{
	public int x = 10;
	public int y = 11;
	public double z = 12;

	public static Object getSomeField(Object o, String fieldName) throws Exception {
		assert(o instanceof B);
		Class c = Class.forName("B");
		Field f = c.getField(fieldName);
		return f.get(o);
	}
}
