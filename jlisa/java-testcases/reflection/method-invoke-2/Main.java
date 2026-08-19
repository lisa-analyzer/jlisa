import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		Class c = Class.forName("B");
		B b = new B();

		assert(b.x == 0);
		assert(b.y.equals("ctor"));
		assert(b.z == true);

		Method method = c.getMethod("foo", new Class[] {int.class});
		assert(method.getName().equals("foo"));
		assert(method.getReturnType() == double.class);
		// assert(method.getParameterTypes().length == 1);

		Integer i = Integer.valueOf(10);

		Object res1 = method.invoke(b, new Object[] {i});
		assert(res1 instanceof Double);
		Double res1Cast = (Double)res1;
		assert(res1Cast.doubleValue() == 10.0);
		assert(b.x == 15);


		Method method2 = c.getMethod("foo2", new Class[] {int.class});

		Integer i2 = Integer.valueOf(11);

		Object res2 = method2.invoke(b, new Object[] {i2});
		assert(res2 instanceof Double);
		Double res2Cast = (Double)res2;

		assert(b.x == 15);
		assert(b.y.equals("ctor"));
		assert(b.z == false);


		method = c.getMethod("bar", new Class[0]);
		assert(method.getName().equals("bar"));
		assert(method.getReturnType() == void.class);
		// assert(method.getParameterTypes().length == 0);

		Object res3 = method.invoke(b, new Object[0]);
		assert(res3 == null);
		assert(b.x == 15);
		assert(b.z == true);
		assert(b.y.equals("bar"));


		method = c.getMethod("getStr", new Class[]{int.class});
		assert(method.getName().equals("getStr"));
		assert(method.getReturnType() == java.lang.String.class);

		i2 = Integer.valueOf(30);
		Object res4 = method.invoke(b, new Object[] {i2});
		assert(res4 instanceof String);
		String res4Cast = (String) res4;
		assert(res4Cast.equals("30"));

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

	public B() {
		super();
		y = "ctor";
		z = true;
	}

	public void bar() {
		z = true;
		y = "bar";
	}

	public static Object baz(Object x) {
		return null;
	}

	public double foo2(int i) {
		z = false;
		return i;
	}
}


