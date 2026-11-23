import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// the field is read only where a is not null: the assertion holds
public class Main {
  static class A { int f; }

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    A a = n ? null : new A();
    try {
      int x = a.f;
      assert !n;
    } catch (NullPointerException e) {
    }
  }
}
