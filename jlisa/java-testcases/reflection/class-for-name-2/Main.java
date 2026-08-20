import java.lang.reflect.Field;

public class ReflectionTest {

	public static void main(String[] args) throws Exception {
		Class c = Class.forName("ReflectionTest$ReflectivelyCreated");
		Class c2 = Class.forName("ReflectionTest");

		assert(c.getName().equals("ReflectionTest$ReflectivelyCreated"));
		assert(c2.getName().equals("ReflectionTest"));
		assert(c != c2);

		Class c3 = ReflectionTest.class;

		assert(c3.getName().equals("ReflectionTest"));
		assert(c2 == c3);

		Class c4 = int.class;
		Class c5 = double.class;

		assert(c4 != c5);

		assert(c4.getName().equals("int"));
		assert(c5.getName().equals("double"));
	}

	class ReflectivelyCreated {
		int innerValue;
	}

	public ReflectivelyCreated rc;

  }

