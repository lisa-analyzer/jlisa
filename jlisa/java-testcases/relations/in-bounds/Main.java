public class Main {

	public static void main(String[] args) {
		int n = args.length;
		int[] a = new int[n];
		for (int i = 0; i < n; i++)
			a[i] = i; // in bounds: i < n, and n is the length

		int s = 0;
		for (int i = 0; i < a.length; i++)
			s += a[i]; // in bounds: i < a.length

		int j = n - 1;
		if (j >= 0)
			a[j] = 0; // in bounds: j = n - 1 < n
	}
}
