public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Catt");
		Class c = Class.forName(s);
	}
}

class Cat {
	private String name;

	public Cat(String x) {
		name = x;
	}
}

