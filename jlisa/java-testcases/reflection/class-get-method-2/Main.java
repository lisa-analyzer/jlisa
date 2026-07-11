import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Class c2 = c.getField("foo").getType(); // expected Foo

		Class c3 = Class.forName("java.lang.Object");

		Method m = c2.getMethod("bar", new Class[]{c3});

		String returnTypeStr = m.getReturnType().getName(); // expected "void"

		Class c4 = Class.forName("Felid");
		Method m2 = c.getMethod("baz", new Class[] {c4});
		String m2Name = m2.getName();

		Method m3 = c.getMethod("bazz", new Class[] {c4}); // expected NoSuchMethodException
		String m3Name = m2.getName();

		return;
	}
}

class Felid {

	void baz(Felid f) { }

}

class Cat extends Felid {
	private int age;

	private Foo foo;

	public Cat() {
		age = 0;
	}

	public void foo(int[] x) { }

	public String[] bar(Integer x) { return new String[0]; }
}

class Foo {

	public void bar(Object x) { }

}
