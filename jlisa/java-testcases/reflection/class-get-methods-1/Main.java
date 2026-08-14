import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Cat cat = new Cat();

		Method[] methods = c.getMethods();

		int methodCount = methods.length;

		// String firstMethodName = methods[0].getName().toString();
		// int i = 0;
		// if (args.length == 1){
		// 	i = 2;
		// }

		Method method = methods[1];

		Object o = method.invoke(cat, new Object[0]);

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


