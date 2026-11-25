// the elements of an array are visited by for-each, in order
public class Main {
  public static void main(String[] args) {
    int[] a = { 1, 2, 3 };
    int sum = 0;
    for (int v : a)
      sum += v;
    assert sum == 6;
  }
}
