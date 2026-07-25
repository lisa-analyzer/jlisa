import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Cat cat = new Cat();

		assert(cat.nickname.equals("ziggy"));
		assert(cat.age == 1);

		Method method = c.getMethod("foo", new Class[0]);
		assert(method.getName().equals("foo"));
		assert(method.getDeclaringClass() == c);
		assert(method.getReturnType() == int.class);
		// assert(method.getParameterTypes().length == 0);

		Object[] os = new Object[0];
		Object res = method.invoke(cat, os);
		assert(res instanceof Integer);
		Integer castRes = (Integer)res;
		assert(castRes.intValue() == 42);

		assert(cat.age == 2);
		assert(cat.nickname.equals("ron"));

		Method method2 = c.getMethod("bar", new Class[0]);
		Object barResult = method2.invoke(cat, new Object[0]);
		assert(barResult == null);

		assert(cat.age == 3);
		assert(cat.nickname == null);

		return;
	}
}

class Cat {
	public String nickname;
	public int age;

	public Cat() {
		nickname = "ziggy";
		age = 1;
	}

	public int foo() {
		nickname = "ron";
		age = 2;
		return 42;
	}

	public void bar() {
		age = 3;
		nickname = null;
	}
}


