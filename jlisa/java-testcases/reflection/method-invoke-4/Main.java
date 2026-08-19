import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s;
		if (args.length == 1)
			s = "foo";
		else
			s = "bar";

		Class c = Class.forName("A");

		Method method = c.getMethod(s, new Class[0]);
		A a = new A();

		Object o = method.invoke(a, new Object[0]);

		assert(o instanceof String);

		return;
	}
}

class A {
	int x = 0;
	String innerString = "hello";

	public String foo() {
		return String.valueOf(x);
	}

	public String bar() {
		return innerString;
	}
}



