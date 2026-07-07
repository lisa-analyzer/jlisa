import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Cat");
		Class c = Class.forName(s);

		Cat cat = new Cat();

		Field f1 = c.getField("age");

		Object catAge = f1.get(cat);

		// if (args.length == 3) {
		// 	cat.nickname = "ron";
		// }
		// else {
		// 	cat.nickname = "jimmy";
		// }

		// String nickname = c.getField("nickname").get("nickname");

		return;
	}
}

class Cat {
	public String nickname;
	public Integer age;

	Cat() {
		nickname = "ziggy";
		age = 5;
	}
}
