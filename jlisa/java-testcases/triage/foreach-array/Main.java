// the elements of an array are visited by for-each, in order
public class Main {
  public static void main(String[] args) {
    int[] a = { 1, 2, 3 };
    for (int v : a)
      if (v == 3)
        assert false;
  }
}
