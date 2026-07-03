public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("ReflectionTest$Foo");
		Class c = Class.forName(s);
		Object o = c.newInstance();
	}

	class Foo {
		private int zoo;
	}
}

