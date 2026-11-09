import java.util.concurrent.atomic.AtomicInteger;

public class Main {

	public static void main(String[] args) {
		AtomicInteger counter = new AtomicInteger(1);
		assert counter.get() == 1;

		counter.set(0);
		assert counter.get() == 0;

		counter.set(7);
		assert counter.get() == 7;
		assert counter.get() == 0; // does not hold
	}
}
