import java.util.Random;

;public class Main {

	public static void main(String[] args) {
		// narrowing, singleton, fits in target range: exact, no truncation
		int a = 100;
		byte a2 = (byte) a;

		// narrowing, singleton, positive overflow, stays positive after wrap
		short b = (short) 1000000;

		// narrowing, singleton, positive overflow, wraps into negative
		byte c = (byte) 200;

		// narrowing to an unsigned type, negative source wraps to a large positive
		char d = (char) -1;

		// widening: always exact, no refinement needed
		byte e = 10;
		int e2 = e;

		// narrowing, singleton, long -> int overflow
		long f = 5000000000L;
		int f2 = (int) f;

		// narrowing, singleton, long -> int, fits exactly
		long g = 42L;
		int g2 = (int) g;

		// narrowing, singleton, exactly at the target's upper bound: fits, no wrap
		short i3 = (short) 32767;

		// narrowing, singleton, one past the target's upper bound: wraps to the
		// minimum
		short i2 = (short) 32768;

		// narrowing, non-singleton/unknown source: cannot be precise, must fall
		// back to the target type's full range
		int h = new Random().nextInt();
		short h2 = (short) h;
	}
}
