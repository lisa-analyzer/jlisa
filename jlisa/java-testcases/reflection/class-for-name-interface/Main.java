import java.lang.reflect.Method;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("MyInterface");

		Method[] methods = c.getMethods();

		int methodCount = methods.length;
	}
}

interface MyInterface {
	void foo();
	int bar(String s);
}