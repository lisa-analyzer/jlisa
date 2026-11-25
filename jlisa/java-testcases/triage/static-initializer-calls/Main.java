import org.sosy_lab.sv_benchmarks.Verifier;

class Counter {
  static int n = 0;
}

class Helper {
  static void bump() {
    Counter.n = 5;
  }
}

class B {
  static int y = 1;

  // writes a static field of another class through a callee, when B is
  // first used
  static {
    Helper.bump();
  }
}

class C {
  static int z;

  // calls only the libraries
  static {
    z = Math.abs(-3);
  }
}

class D extends C {
  static int w = 2;
}

class E extends B {
  static int v = 3;
}

public class Main {
  static int touch(int n) {
    if (n > 0)
      return B.y + touch(n - 1);
    return 0;
  }

  public static void main(String[] args) {
    int read = Counter.n;
    int n = Verifier.nondetInt();
    if (n > 0 && n < 4) {
      touch(n);
      assert Counter.n == read;
    }
    int u = D.w + E.v;
  }
}
