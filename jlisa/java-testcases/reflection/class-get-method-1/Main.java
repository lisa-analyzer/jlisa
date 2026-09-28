import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Cat");
		Class c = Class.forName(s);
		assert(c.getName().equals("Cat"));

		Class c1 = Class.forName("java.lang.Integer");
		assert(c1.getName().equals("java.lang.Integer"));

		Method m1 = c.getMethod("foo", new Class[] {float.class});
		assert(m1.getName().equals("foo"));
		assert(m1.getReturnType() == int.class);
		assert(m1.getDeclaringClass() == c);

		Class[] methodParams = m1.getParameterTypes();
		assert(methodParams[0] == float.class);


		Method method = c.getMethod("bar", new Class[] {c1});
		assert(method.getName().equals("bar"));
		assert(method.getDeclaringClass() == c);

		Class[] methodParams2 = method.getParameterTypes();
		assert(methodParams2[0] == c1);

		Class methodRetType = method.getReturnType();
		String retTypeName = methodRetType.getName().toString();
		assert(retTypeName.equals("java.lang.String[]"));

		return;
	}
}

class Cat {
	private int age;

	public Cat() {
		age = 0;
	}

	public int foo(float x) { return 42.0f; }

	public String[] bar(Integer x) { return new String[0]; }
}


