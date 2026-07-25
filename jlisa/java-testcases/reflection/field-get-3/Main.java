import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {

		A a = new A();

		assert(a.name.equals("ziggy"));

		try {
			Object o = B.getAField(a);
			assert(o instanceof String);
			String str = (String)o;
			assert(str.equals("ziggy"));
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
	public double z = 11;

	public static Object getAField(Object o) throws Exception {
		assert(o instanceof A);
		Class c = Class.forName("B");
		Field f = c.getField("name");
		return f.get(o);
	}
}
