import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Class c2 = Animal.class;

		Field f1 = c.getField("nickname");
		Field f2 = c.getField("age");
		Field f3 = c.getField("pi");

		assert(f1.getName().equals("nickname"));
		assert(f2.getName().equals("age"));
		assert(f3.getName().equals("pi"));

		assert(f1.getDeclaringClass() == c);
		assert(f2.getDeclaringClass() == c2);
		assert(f3.getDeclaringClass() == c2);

		assert(f1.getType().getName().equals("java.lang.String"));
		assert(f2.getType().getName().equals("int"));
		assert(f3.getType().getName().equals("double"));

		// should check that
		// getModifiers() & Modifier.STATIC == Modifier.STATIC
		assert(f3.getModifiers() != 0);

		return;
	}
}

class Animal {
	public static double pi = 3.14;
	public int age;
}

class Cat extends Animal {

	public String nickname;

	public Cat(String x) {
		nickname = x;
		age = 0;
	}

	public Cat() {
		nickname = "ziggy";
		age = 0;
	}
}

