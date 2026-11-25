import org.sosy_lab.sv_benchmarks.Verifier;

class A {
  static int x = 0;
}

class B extends A {
}

class C extends B {
  // writes the static field inherited from A, recursively
  static void set(int n) {
    if (n > 0) {
      x = 5;
      set(n - 1);
    }
  }
}

public class Main {
  public static void main(String[] args) {
    A.x = 0;
    int n = Verifier.nondetInt();
    if (n > 0 && n < 4) {
      C.set(n);
      assert A.x == 0;
    }
  }
}
