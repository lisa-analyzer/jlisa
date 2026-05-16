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

class Cat {
	private String nickname;

	public Cat(String x) {
		nickname = x;
	}

	public Cat() {
		nickname = "ziggy";
	}
}

