import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Method[] methods = c.getMethods();

		int methodCount = methods.length;

		String firstMethodName = methods[0].getName().toString();

		return;
	}
}

class Cat {
	private int age;

	public Cat() {
		age = 0;
	}

	public void foo() { }

	public void bar() { }
}


