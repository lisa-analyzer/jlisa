import java.lang.reflect.Field;

public class Main {
	public static void main(String[] args) throws Exception {
		Class clazz = Class.forName("Holder");

		assert(Holder.count == 0);
		assert(Holder.label == null);
		assert(Holder.branchCount == 0);

		Field count = clazz.getField("count");
		Field label = clazz.getField("label");
		Field branchCount = clazz.getField("branchCount");

		count.set(null, Integer.valueOf(7));

		assert(Holder.count == 7);

		if (args.length > 0) {
			label.set(null, "left");
			branchCount.set(null, Integer.valueOf(1));
			assert(Holder.label.equals("left"));
			assert(Holder.branchCount == 1);
		} else {
			label.set(null, "right");
			branchCount.set(null, Integer.valueOf(2));
			assert(Holder.label.equals("right"));
			assert(Holder.branchCount == 2);
		}

		assert(Holder.label.equals("right")); // possible
		assert(Holder.branchCount == 2); // possible

	}
}

class Holder {
	public static int count = 0;
	public static String label = null;
	public static int branchCount = 0;
}
