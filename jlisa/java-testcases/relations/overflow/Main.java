public class Main {

	public static void main(String[] args) {
		int n = args.length;
		int m = n + 3;
		int[] b = new int[m];
		b[n] = 1; // may throw: n + 3 might overflow
	}
}
