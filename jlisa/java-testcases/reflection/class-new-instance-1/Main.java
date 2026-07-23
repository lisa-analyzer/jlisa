import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c1 = Class.forName(s);
		Class c2 = Class.forName("Felid");
		Class c3 = Class.forName("Mammal");

		assert(c1 != c2);
		assert(c2 != c3);

		Object o1 = c1.newInstance();
		Object o2 = c2.newInstance();
		Object o3 = c3.newInstance();

		assert(o1 instanceof Cat);
		assert(o2 instanceof Felid);
		assert(o3 instanceof Mammal);

		assert(o2 instanceof Mammal);
		assert(!(o2 instanceof Cat));

		assert(!(o3 instanceof Felid));
		assert(!(o3 instanceof Cat));

		return;
	}
}

interface Animal {
}

class Mammal implements Animal {
	private boolean baz = true;
}

class Felid extends Mammal {
	private int boo = 42;
}

class Cat extends Felid {
	private double foo = 10;
}

