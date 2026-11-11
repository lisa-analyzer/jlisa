import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static class A {
  }

  static class B extends A {
  }

  public static void main(String[] args) {
    boolean f = Verifier.nondetBoolean();
    A a = f ? new B() : null;
    // a is not null when f holds: c is either null or an instance of B
    A c = f && a == null ? new A() : a;
    // the cast never fails: casting null succeeds
    B b = (B) c;
  }
}
