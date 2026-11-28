import org.sosy_lab.sv_benchmarks.Verifier;

class Counter {
  int value;

  void increment() {
    value = value + 1;
  }
}

public class Main {
  public static void main(String[] args) {
    Counter c = new Counter();
    int n = Verifier.nondetInt();
    if (n > 0 && n < 100) {
      c.value = n;
      c.increment();
      assert c.value > 1;
    }
  }
}
