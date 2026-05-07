public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("xyz");
		assert s.equals("xyz");

		Class c = Class.forName(s);
	}
}

