import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Class c2 = Felid.class;
		Class c3 = Animal.class;

		Field f = c.getField("nickname");
		assert(f.getName().equals("nickname"));
		assert(f.getDeclaringClass() == c);
		assert(f.getType() == java.lang.String.class);


		Field f2 = c.getField("foo");
		assert(f2.getName().equals("foo"));
		assert(f2.getDeclaringClass() == c);
		assert(f2.getType() == Foo.class);

		Class fooClass = f2.getType();
		assert(fooClass == Foo.class);

		Field f3 = fooClass.getField("fooValue");
		assert(f3.getName().equals("fooValue"));

		Field f4 = c.getField("hasWings");
		assert(f4.getName().equals("hasWings"));
		assert(f4.getDeclaringClass() == Animal.class);

		try {
			Field noSuchField = c.getField("hasLegs");
		}
		catch (NoSuchFieldException e) {
			assert false;
		}

		try {
			Class tmp = null;
			tmp.getField("hasLegs");
		}
		catch (NullPointerException e) {
			assert false;
		}

		return;
	}
}

class Animal {
	private boolean hasWings;
}

class Felid extends Animal {
	private float height;
}


class Cat extends Felid {
	private String nickname;

	private Foo foo;

	public Cat(String x) {
		nickname = x;
	}

	public Cat() {
		nickname = "ziggy";
	}
}

class Foo {
	public double fooValue;
}

