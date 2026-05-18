import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Field f = c.getField("nickname");

		return;
	}
}

class Cat {
	private String nickname;

	public Cat(String x) {
		nickname = x;
	}

	public Cat() {
		nickname = "ziggy";
	}
}

