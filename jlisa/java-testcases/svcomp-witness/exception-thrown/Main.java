public class Main {
  static void fail() {
    throw new IllegalStateException();
  }

  public static void main(String[] args) {
    fail();
  }
}
