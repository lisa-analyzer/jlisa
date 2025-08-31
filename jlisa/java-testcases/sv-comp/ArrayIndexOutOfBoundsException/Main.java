public class Main {
  public static void main(String args[]) {
    int size = Verifier.nondetInt();
    if (size < 0) return;
    try {
      int[] a = new int[4];
      a[size] = 0;
    } catch (ArrayIndexOutOfBoundsException exc) {
      assert false;
    }
  }
}