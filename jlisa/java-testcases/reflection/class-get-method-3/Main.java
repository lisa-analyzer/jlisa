import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Method m = c.getMethod("canFly", new Class[0]);

		String methodName = m.getName();
		Class returnType = m.getReturnType();


		Class intClass = c.getField("age").getType();

		Method m2 = c.getMethod("jump", new Class[] {intClass});
		methodName = m2.getName();
		returnType = m2.getReturnType();

		return;
	}
}

interface Animal {
	public boolean canFly();
}

class Felid {
	public int jump(int x) { return 1;}
}

class Cat extends Felid implements Animal {
	private int age;

	public Cat() {
		age = 0;
	}

	public void foo(int[] x) { }

	public String[] bar(Integer x) { return new String[0]; }

	public boolean canFly() {
		return false;
	}

}


