public class Main {

	static int divide(int x, int y) {
		x /= y;
		return x;
	}

	public static void main(String[] args) {
		int x = 1;
		int y = 0;
		try {
			x /= y;
		} catch (ArithmeticException e) {
			x = 5;
		}
		assert x == 5;

		int z = 1;
		try {
			z = divide(z, y);
		} catch (ArithmeticException e) {
			z = 7;
		}
		assert z == 7;
	}
}
