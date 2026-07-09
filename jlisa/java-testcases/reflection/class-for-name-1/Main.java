public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Foo");
		Class c = Class.forName(s);
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

