import java.lang.reflect.Field;

public class ReflectionTest {

	public static void main(String[] args) throws Exception {
		Class c = Class.forName("ReflectionTest$ReflectivelyCreated");
		Class c2 = Class.forName("ReflectionTest");

		String innerName = c.getName().toString();

		String outerName = c2.getName().toString();

		Field innerValueField = c.getField("innerValue");
		String fieldName1 = innerValueField.getName().toString();
		Class fieldType1 = innerValueField.getType();

		Field outerValueField = c2.getField("rc");
		String fieldName2 = outerValueField.getName().toString();
		Class fieldType2 = outerValueField.getType();
	}

	class ReflectivelyCreated {
		int innerValue;
	}

	public ReflectivelyCreated rc;

  }

