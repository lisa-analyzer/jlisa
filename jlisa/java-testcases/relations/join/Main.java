public class Main {

	public static void main(String[] args) {
		int n = args.length;
		int[] a = new int[n];
		int i = n;
		if (n > 5)
			i = n - 1; // i < n on this branch only
		a[i] = 0; // may throw: on the other branch i is the length
	}
}
