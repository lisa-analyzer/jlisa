import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Field f1 = c.getField("nickname");
		Field f2 = c.getField("alias");

		Cat cat = new Cat("whiskers", "fluffyAlias");

		Object val = f1.get(cat);
		f1.set(cat, "fluffy");

		Object val2 = f2.get(cat);
		f2.set(cat, "newAlias");

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
