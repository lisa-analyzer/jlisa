import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.sv_benchmarks.Verifier;

// a class of the program with fields named as the ones of library classes
class Box {
  int value;
  int capacity;
}

public class Main {
  static void bump(AtomicInteger a, int n) {
    if (n > 0) {
      a.set(a.get() + 1);
      bump(a, n - 1);
    }
  }

  public static void main(String[] args) {
    Box b = new Box();
    b.value = 1;
    AtomicInteger a = new AtomicInteger(0);
    int n = Verifier.nondetInt();
    if (n > 0 && n < 4) {
      bump(a, n);
      assert a.get() == 0;
    }
  }
}
