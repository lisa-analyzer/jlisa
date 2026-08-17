public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Foo");
		Class c = Class.forName(s);

		assert(c.getName().equals("Foo"));

		Class c2 = Class.forName("Foo");
		assert(c == c2);

		assert(c == Foo.class);
	}
}

class Baz {
	public String bazString;
}


class Foo extends Baz {

	public int zz;

	public static String x = "hello";
	public static double pi = 3.14;

	public int[] ages;

	public String[] nicknames;
}


interface A {
	static int foo1 = 42;
}

