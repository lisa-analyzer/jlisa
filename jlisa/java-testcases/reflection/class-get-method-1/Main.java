import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Class c1 = Class.forName("java.lang.Integer");

		Method method = c.getMethod("bar", new Class[] {c1});
		String methodName = method.getName();

		Class[] methodParams = method.getParameterTypes();

		Class methodRetType = method.getReturnType();

		return;
	}
}

class Cat {
	private int age;

	public Cat() {
		age = 0;
	}

	public void foo(int[] x) { }

	public String[] bar(Integer x) { return new String[0]; }
}


