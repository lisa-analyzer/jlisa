import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c1 = Class.forName(s);
		Class c2 = Class.forName("Foo");

		Method m1 = c2.getMethod("bar", new Class[]{java.lang.Object.class});

		assert(m1.getName().equals("bar"));
		assert(m1.getReturnType() == void.class);
		assert(m1.getDeclaringClass() == Foo.class);

		Method m2 = c1.getMethod("baz", new Class[] {Felid.class});
		assert(m2.getName().equals("baz"));
		assert(m2.getReturnType() == int.class);
		assert(m2.getDeclaringClass() == Felid.class);

		try {
			Method m3 = c1.getMethod("bazz", new Class[0]); // noSuchMethod
		}
		catch (NoSuchMethodException e) {
			assert false;
		}

		return;
	}
}

class Felid {

	int baz(Felid f) { return 42; }

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
