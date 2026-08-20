import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s;
		if (args.length == 1)
			s = "bazString";
		else
			s = "f3";

		Class c = Class.forName("Foo");
		Field f = c.getField(s);

		Foo foo = new Foo();
		Object o = f.get(foo);
	}
}

class Baz {
	public String bazString = "hello";
}


class Foo extends Baz {
	public int f1 = 1;
	public double f2 = 2.0;
}

