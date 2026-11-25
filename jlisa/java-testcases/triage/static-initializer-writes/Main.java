import org.sosy_lab.sv_benchmarks.Verifier;

class A {
  static int x = 0;
}

class B {
  static int y = 1;

  // writes a static field of another class, when B is first used
  static {
    A.x = 5;
  }
}

public class Main {
  static int touch(int n) {
    if (n > 0)
      return B.y + touch(n - 1);
    return 0;
  }

  public static void main(String[] args) {
    int read = A.x;
    int n = Verifier.nondetInt();
    if (n > 0 && n < 4) {
      touch(n);
      assert A.x == read;
    }
  }
}
