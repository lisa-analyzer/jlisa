public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Object o = c.newInstance();

		return;
	}
}

interface Animal {
}

class Mammal {
	private static boolean foo = true;
}

class Felid extends Mammal {
	private static int boo = 42;
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

