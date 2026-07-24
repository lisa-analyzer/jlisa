import java.lang.reflect.Method;
import java.lang.Math;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {
		String s = new String("Cat");
		Class c = Class.forName(s);

		Method m1 = c.getMethod("canFly", new Class[0]);
		assert(m1.getDeclaringClass() == c);
		assert(m1.getReturnType() == boolean.class);
		assert(m1.getName().equals("canFly"));

		Method m2 = c.getMethod("jump", new Class[] {int.class});
		assert(m2.getDeclaringClass() == Felid.class);
		assert(m2.getReturnType() == int.class);
		assert(m2.getName().equals("jump"));

		Method m3 = c.getMethod("furColor", new Class[] {boolean.class});
		assert(m3.getDeclaringClass() == Cat.class);
		assert(m3.getReturnType() == java.lang.String.class);
		assert(m3.getName().equals("furColor"));

		return;
	}
}

interface Animal {
	public boolean canFly();
}

interface Animal2 {
	public String furColor(boolean b);
}

class Felid {
	public int jump(int x) { return 1;}
}

class Cat extends Felid implements Animal, Animal2{
	private int age;

	public Cat() {
		age = 0;
	}

	public void foo(int[] x) { }

	public String[] bar(Integer x) { return new String[0]; }

	public boolean canFly() {
		return false;
	}

	public String furColor(boolean b) {return "orange";}

}


