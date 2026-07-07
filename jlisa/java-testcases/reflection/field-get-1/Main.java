import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Cat");
		Class c = Class.forName(s);

		Cat cat = new Cat();

		Field f1 = c.getField("age");

		Object catAge = f1.get(cat);

		Field f2 = c.getField("agePrimitive");

		Object catAgePrimitive = f2.get(cat);

		return;
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
