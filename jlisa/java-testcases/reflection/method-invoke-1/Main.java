import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Method method = c.getMethod("foo", new Class[0]);

		Cat cat = new Cat();

		String x = "ron";

		Object zz = method.invoke(cat, new Object[0]);

		return;
	}
}

class Cat {
	public String nickname;
	public int age;

	public Cat() {
		nickname = "ziggy";
		age = 90;
	}

	public int foo() {
		nickname = "gatto";
		age = 91;
		return 42;
	}
}


