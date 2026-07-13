import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("Cat");

		Field f = c.getField("name");
		Object result = f.get(null);
	}
}

class Cat {
	public String name = "fluffy";
}