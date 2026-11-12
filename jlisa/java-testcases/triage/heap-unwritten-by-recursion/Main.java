import org.sosy_lab.sv_benchmarks.Verifier;

class Counter {
  int kept;
  int steps;

  void run(int n) {
    if (n > 0) {
      steps = steps + 1;
      run(n - 1);
    }
  }
}

public class Main {
  public static void main(String[] args) {
    Counter c = new Counter();
    int v = Verifier.nondetInt();
    int n = Verifier.nondetInt();
    c.kept = v;
    if (n > 0 && n < 4) {
      c.run(n);
      assert c.kept == v;
    }
  }
}
