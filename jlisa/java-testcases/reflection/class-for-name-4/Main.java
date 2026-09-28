import java.lang.reflect.Field;

public class ReflectionTest {

	public static void main(String[] args) throws Exception {

		Class c1 = Class.forName("C");
		assert(c1 == C.class);
		assert(c1.getName().equals("C"));

		assert(C.str1.equals("string1"));
		assert(C.str2.equals("string2"));
		assert(C.int1 == 1);
		assert(C.int2 == 2);
		assert(C.double1 == 42.0);
		assert(B.float1 == 50.0);
		assert(Y.bool1 == true);

		C.foo();

		assert(C.str2.equals("string2new"));

	}

	class ReflectivelyCreated { // not used
		int innerValue;
	}

	public static Class getZ() { // not used
		return Class.forName("Z");
	}
}

interface A {
	public void a();
}

interface Z { }

class Y {
	public static boolean bool1 = true;
}

class B extends Y implements A {

	public static float float1 = 50.0;

	public void a() {}
	public void b() {}
}

class C extends B implements Z {

	public static String str1 = "string1";
	public static String str2 = "string2";
	public static int int1 = 1;
	public static int int2 = 2;
	public static double double1 = 42.0;

	public int c() { return 0; }

	public static void foo() {
		C.str2 = "string2new";
	}
}

