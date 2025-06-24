public class AssertExample {
    public static void main(String[] args) {
        int number = -5;

        assert true;
		assert number >= 0 : "Number must be non-negative";
    }
}
