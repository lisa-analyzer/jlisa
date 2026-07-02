import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Method fooMethod = c.getMethod("foo", new Class[0]);
		String methodName = fooMethod.getName();

		Class[] methodParams = fooMethod.getParameterTypes();

		return;
	}
}

class Cat {
	private int age;

	public Cat() {
		age = 0;
	}

	public void foo() { }

}


