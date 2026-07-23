import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		assert(c == Cat.class);

		Cat cat = new Cat("whiskers", "fluffyAlias");
		assert(cat.nickname.equals("whiskers"));
		assert(cat.alias.equals("fluffyAlias"));

		Field f1 = c.getField("nickname");

		Object val = f1.get(cat);
		assert(val instanceof String);

		f1.set(cat, "fluffy");
		assert(cat.nickname.equals("fluffy"));

		Field f2 = c.getField("alias");

		Object val2 = f2.get(cat);
		assert(val2 instanceof String);

		f2.set(cat, "newAlias");
		assert(cat.alias.equals("newAlias"));

		return;
	}
}

class Cat {
	public String nickname;
	public String alias;

	public Cat(String x, String a) {
		nickname = x;
		alias = a;
	}

	public Cat() {
		nickname = "ziggy";
		alias = null;
	}
}
