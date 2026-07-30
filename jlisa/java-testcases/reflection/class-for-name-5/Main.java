import java.lang.reflect.Field;

import inner.*;

public class ReflectionTest {

	public static void main(String[] args) throws Exception {

		Class c1 = Class.forName("inner.Base");
		assert(c1 == Base.class);
		assert(c1.getName().equals("inner.Base"));

		Class c2 = Class.forName("inner.B");
		assert(c2 == B.class);
		assert(c2.getName().equals("inner.B"));

		Class c3 = Class.forName("inner.C");
		assert(c3 == C.class);
		assert(c3.getName().equals("inner.C"));
	}

	class ReflectivelyCreated { // not used
		int innerValue;
	}

	public static Class getZ() { // not used
		return Class.forName("Z");
	}
}


class A {}

