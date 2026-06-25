import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Field f1 = c.getField("nickname");

		Field f2 = c.getField("age");

		Field f3 = c.getField("pi");

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

