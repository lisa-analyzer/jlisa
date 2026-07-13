import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("Cat");

		Field f = c.getField("name");
		f.set(null, "newName");
	}
}

class Cat {
	public String name = "fluffy";
}