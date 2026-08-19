import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("Cat");

		String s;
		if (args.length == 1)
			s = "name";
		else
			s = "nickname";

		Field f = c.getField(s);
		f.set(null, "newName");

		assert(Cat.name.equals("newName"));
		assert(Cat.nickname.equals("newName"));
	}
}

class Cat {
	public static String name = "fluffy";
	public static String nickname = "fluff";
}
