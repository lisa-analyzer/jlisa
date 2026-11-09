public class Main {
  static int[] values = new int[1];

  static int read(int i) {
    int value = values[i];
    return value;
  }

  public static void main(String[] args) {
    read(1);
  }
}
