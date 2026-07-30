public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("ReflectionTest$Foo");
		Class c = Class.forName(s);
		assert(c.getName().equals("ReflectionTest$Foo"));

		Object o = c.newInstance();
		assert(o instanceof ReflectionTest.Foo);

		ReflectionTest.Foo f = (ReflectionTest.Foo) o;
		assert(f.bar == 42);
	}

	static class Foo {
		public int bar;

		Foo() {
			bar = 42;
		}
	}
}

