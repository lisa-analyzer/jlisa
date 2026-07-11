import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Field f2 = c.getField("nickname");

		Field fooField = c.getField("foo");
		String fooFieldName = fooField.getName().toString(); // expected "foo"

		Class fooClass = fooField.getType(); // expected Class Foo

		Field fooValueField = fooClass.getField("fooValue");
		String fooValueFieldName = fooValueField.getName().toString(); // expected "fooValue"

		Field superclassField1 = c.getField("height"); // expected Field height

		Field superClassField2 = c.getField("hasWings");

		Field noSuchField = c.getField("hasLegs");

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

