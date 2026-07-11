import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class c = Class.forName("Holder");

		// int primitive static field
		Field fi = c.getField("staticInt");
		Object vi = fi.get(null);

		// double primitive static field
		Field fd = c.getField("staticDouble");
		Object vd = fd.get(null);

		// boolean primitive static field
		Field fb = c.getField("staticBool");
		Object vb = fb.get(null);

		// null static field (reference type)
		Field fnull = c.getField("staticNullString");
		Object vnull = fnull.get(null);

		// final static field
		Field ffinal = c.getField("staticFinalInt");
		Object vfinal = ffinal.get(null);

		// Integer wrapper static field
		Field fw = c.getField("staticWrapper");
		Object vw = fw.get(null);
	}
}

class Holder {
	public static int staticInt = 10;
	public static double staticDouble = 2.71;
	public static boolean staticBool = false;
	public static String staticNullString = null;
	public static final int staticFinalInt = 99;
	public static Integer staticWrapper = 50;
}