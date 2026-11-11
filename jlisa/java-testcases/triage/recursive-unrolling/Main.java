public class Main {
  static int fib(int n) {
    if (n < 2) {
      return n;
    }
    return fib(n - 1) + fib(n - 2);
  }

  public static void main(String[] args) {
    int r = fib(25);
    assert r >= 0;
  }
}
