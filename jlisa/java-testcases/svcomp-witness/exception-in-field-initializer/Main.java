public class Main {
  static int[] values = new int[1];

  static class Foo {
    int y = values[3];
  }

  public static void main(String[] args) {
    new Foo();
  }
}
