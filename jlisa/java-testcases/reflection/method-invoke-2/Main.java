import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		Class c = Class.forName("B");
		B b = new B();

		Method method = c.getMethod("foo", new Class[] {int.class});
		assert(method.getName().equals("foo"));
		assert(method.getReturnType() == double.class);

		Integer i = Integer.valueOf(10);

		Object zz = method.invoke(b, new Object[] {i});

		assert(b.y.equals("ctor"));
		assert(b.z == false);

		assert(zz instanceof Double);

		return;
	}
}

class A {
	int x = 0;
}

class B extends A {

	public String y;
	public boolean z;

	public B() {
		y = "ctor";
		z = true;
	}

	public double foo(int i) {
		z = false;
		return i;
	}

	// public void bar() {
	// 	age = 92;
	// }
	//
	// public static Object baz() {
	// 	return null;
	// }
}


