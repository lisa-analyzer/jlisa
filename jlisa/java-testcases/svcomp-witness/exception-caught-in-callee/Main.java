public class Main {
  static int[] values = new int[1];

  static void read() {
    try {
      values[3] = 0;
    } catch (ArrayIndexOutOfBoundsException e) {
      values[0] = 1;
    }
    throw new IllegalStateException();
  }

  public static void main(String[] args) {
    read();
  }
}
