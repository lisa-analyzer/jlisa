public class Main {
  static int[] values = new int[1];

  static void fail() {
    throw new IllegalStateException("" + values[3]);
  }

  public static void main(String[] args) {
    fail();
  }
}
