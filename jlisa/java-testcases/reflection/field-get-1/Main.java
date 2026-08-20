import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Cat");
		Class c = Class.forName(s);

		Cat cat = new Cat();

		Field f1 = c.getField("age");
		assert(f1.getName().equals("age"));
		assert(f1.getType() == java.lang.Integer.class);

		getCatAge(cat, f1);

		Object catAge = getFieldValue(cat, f1);
		assert(catAge instanceof Integer);

		Integer catAgeCast = (Integer) catAge;
		//NOTE: some bug is preventing unboxing
		assert(catAgeCast == 5);
		assert(catAgeCast.intValue() == 5);

		Field f2 = c.getField("agePrimitive");
		assert(f2.getName().equals("agePrimitive"));
		assert(f2.getType() == int.class);
		Object catAgePrimitive = getFieldValue(cat, f2);

		assert(catAgePrimitive instanceof Integer);

		Field f3 = c.getField("nickname");
		assert(f3.getName().equals("nickname"));
		assert(f3.getType() == java.lang.String.class);

		Object catNickname = getFieldValue(cat, f3);

		assert(catNickname instanceof String);
		assert(((String)catNickname).equals("ziggy"));

		return;
	}

	private static Object getFieldValue(Object o, Field f) {
		return f.get(o);
	}
}

class Cat {
	public String nickname;
	public Integer age;
	public int agePrimitive;

	Cat() {
		nickname = "ziggy";
		age = 5;
		agePrimitive = 78;
	}
}
