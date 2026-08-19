import java.lang.reflect.Field;

public class ReflectionTest {

	public static void main(String[] args) throws Exception {

		if (args.length == 2) {
			Class c1 = Class.forName("A");
			assert(c1 == A.class);
			assert(c1.getName().equals("A"));
		}
		else { }

		Class c1 = Class.forName("A");
		assert(c1 == A.class);
		assert(c1.getName().equals("A"));

		Class c2 = Class.forName("B");
		assert(c2 == B.class);
		assert(c2.getName().equals("B"));

		Class c3 = Class.forName("C");
		assert(c3 == C.class);
		assert(c3.getName().equals("C"));

		try {
			Class c4 = Class.forName("D");
		}
		catch (ClassNotFoundException e) {
			assert true;
		}

		Class c5 = getZ();
		assert(c5 == Z.class);
		assert(c5.getName().equals("Z"));

	}

	class ReflectivelyCreated {
		int innerValue;
	}

	public static Class getZ() {
		return Class.forName("Z");
	}
}


interface A {
	public void a();
}

interface Z { }

class B implements A {
	public void a() {}
	public void b() {}
}

class C extends B implements Z {
	public int c() { return 0; }
}
