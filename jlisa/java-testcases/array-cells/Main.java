public class Main {

	public static void main(String[] args) {
		writePast64(args.length + 100);
		references(args.length + 100, args.length);
	}

	// a write with an index past the tracked cells may change a[120]
	static void writePast64(int i) {
		int[] a = new int[200];
		if (i < 100 || i >= 150)
			return;
		a[i] = 7;
		assert a[120] == 0;
	}

	// the cell written through i is not the one read through i after i
	// changes
	static void references(int size, int n) {
		Object[] o = new Object[size];
		int i = 0;
		o[i] = new Object();
		i = n;
		if (i < 0 || i >= size)
			return;
		assert o[i] != null;
	}
}
