import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Class stringClazz = Class.forName("java.lang.String");

		Method fooMethod = c.getMethod("bar", new Class[] {stringClazz});
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

	public void bar(String s) { }

}


