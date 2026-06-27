import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionTest {
	public static void main(String[] args) throws Exception {

		String s = new String("Cat");

		Class c1 = Class.forName(s);
		Class c2 = Class.forName("java.lang.String");
		//
		Class[] arr = new Class[] {c1, c2};

		Method m = c1.getMethod("sound", arr);

		Field f1 = c1.getField("nickname");

		// additional field to test multiple field resolution
		Field f2 = c1.getField("alias");

		// String z = f.getName();
		// String zzz = z.toString();

		//Class c2 = f1.getDeclaringClass();

		//int mod = f1.getModifiers();

		Cat cat = new Cat("whiskers", "fluffyAlias");

		// field.get: legge il valore di nickname dall'istanza cat
		Object val = f1.get(cat);

		// field.set: scrive un nuovo valore in nickname sull'istanza cat
		f1.set(cat, "fluffy");

		// test second field
		Object val2 = f2.get(cat);
		f2.set(cat, "newAlias");

		return;
	}
}

class Cat {
	public String nickname;
	public String alias;

	public int age;

	public Cat(String x) {
		nickname = x;
		alias = null;
	}

	public Cat(String x, String a) {
		nickname = x;
		alias = a;
	}

	public Cat() {
		nickname = "ziggy";
	}

	public String sound(Cat x, String y){
		return "meow";
	}

	public String sound(){
		return "meow";
	}

	public String sound(int x){
		return "meow";
	}

	public String sound(int x, int y, int z){
		return "meow";
	}

	public static String sound(int x, int y, int z, int w){
		return "meow";
	}
}

