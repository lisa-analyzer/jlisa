import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c1 = Class.forName(s);

		Object o1 = c1.newInstance();

		Class c2 = Class.forName("Felid");
		Object o2 = c2.newInstance();

		return;
	}
}

interface Animal {
}

class Mammal implements Animal {
	private static boolean foo = true;
}

class Felid extends Mammal {
	private int boo = 42;
}

class Cat extends Felid {
	private String nickname;

	private static int age = 2;

	public Cat(String x) {
		nickname = x;
	}

	public Cat() {
		nickname = "ziggy";
	}
}

