public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s;

		if (args.length == 1)
			s = "A";
		else
			s = "B";

		Class c = Class.forName(s);
		Object o = c.newInstance();

		assert(o instanceof A); // possible
		assert(o instanceof B); // possible
		assert(!(o instanceof C));
	}

}

class A {
	int x = 42;
}

class B {
	String s = "hello";
}

class C { }
