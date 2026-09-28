public class Main {

    public static void main(String[] args) throws Exception {
        Counter counter = new Counter();

        assert (counter.sum == 0);

        counter.run();

        assert (counter.sum == 8);
    }
}

class StepHandler {
    public int compute(int index) {
        return index;
    }
}

class Counter {
    int sum = 0;

    public void run() {

        StepHandler handler = new StepHandler() {

            int x = 1;

            @Override
            public int compute(int y) {
		int tmp = x * y;
		x = tmp;
                return x;
            }
        };

	sum = sum + handler.compute(2);
	sum = sum + handler.compute(3);
    }
}
