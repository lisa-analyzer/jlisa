public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Foo");
		Class c = Class.forName(s);
	}
}

// class Baz {
// 	public static String x = "hello";
// }

class Foo {
	public static String x = "hello";
	public static double pi = 3.14;

}

