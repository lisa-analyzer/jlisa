import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);
		Class intClass = Class.forName("java.lang.Integer");

		Method method = c.getMethod("foo", new Class[] {intClass});

		Cat cat = new Cat();
		Integer x = 43;

		Integer zz = method.invoke(cat, new Object[] {x});

		return;
	}
}

class Cat {
	private Integer age;

	public Cat() {
		age = 0;
	}

	public Integer foo(Integer x) {
		age = x;
		return (age + 5);
	}
}


