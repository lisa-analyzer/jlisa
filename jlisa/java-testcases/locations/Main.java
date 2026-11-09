public class Main {

	static int compute(int a, int b) {
		return a + b;
	}

	public static void main(String[] args) {
		int a = 1;
		int b = 2;
		int c = a + b + 3;
		int d = compute(a,
				b);
		d += c;
	}
}
