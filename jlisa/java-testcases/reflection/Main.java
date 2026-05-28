import java.lang.reflect.Field;

public class ReflectionTest {
	public static void main(String[] args) throws NoSuchMethodException, ClassNotFoundException {
		String s = new String("Cat");
		Class c = Class.forName(s);

		java.lang.reflect.Method m = c.getMethod("sound", "test");

		return;
	}
}

class Cat {
	private String nickname;

	public Cat(String x) {
		nickname = x;
	}

	public Cat() {
		nickname = "ziggy";
	}

	public String sound(int x,int y){
		return "meow";
	}
}

