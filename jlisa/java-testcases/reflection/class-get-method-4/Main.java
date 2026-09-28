import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s;
		if (args.length == 1)
			s = "foo";
		else
			s = "baz";

		Class c = Class.forName("A");
		Method m1 = c.getMethod(s, new Class[0]);

		assert(m1.getDeclaringClass() == c);
		assert(!(m1.getName().equals("bar")));

		return;
	}
}


class A {

	public void foo() {}
	public void baz() {}

}

