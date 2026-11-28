public class Main {
  public static void main(String[] args) {
    double a = args.length > 0 ? 0.1 : 0.5;
    double y = a + 0.7; // 0.7999999999999999 (rounded down) or 1.2
    if (y <= 0.7999999999999999) {
      assert false; // reached with a = 0.1
    }
  }
}
