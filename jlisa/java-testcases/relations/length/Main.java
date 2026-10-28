public class Main {

	public static void main(String[] args) {
		int n = args.length;
		int[] a = new int[n];
		a[n] = 1; // throws: n is the length
	}
}
