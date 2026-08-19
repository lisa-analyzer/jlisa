import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		Class c = Class.forName("B");
		B b = new B();

		assert(b.x == 0);
		assert(b.y.equals("ctor"));
		assert(b.z == true);

		Method method = c.getMethod("baz", new Class[] {int.class});
		assert(method.getName().equals("baz"));
		assert(method.getReturnType() == java.lang.Object.class);

		Integer i = Integer.valueOf(42);
		Object o = method.invoke(null, new Object[] {i});
		assert(o == null);
		assert(B.innerString.equals("helloFromBaz"));

		return;
	}
}

class A {
	int x = 0;

	public String getStr(int x) {
		return String.valueOf(x);
	}

	public double foo(int i) {
		x = 15;
		return i;
	}
}

class B extends A {

	public String y;
	public boolean z;

	public static String innerString = "hello";

	public B() {
		super();
		y = "ctor";
		z = true;
	}

	public void bar() {
		z = true;
		y = "bar";
	}

	public static Object baz(int x) {
		innerString = "helloFromBaz";
		return null;
	}

}


