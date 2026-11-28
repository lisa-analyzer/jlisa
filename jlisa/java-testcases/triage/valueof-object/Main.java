import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Object o = new Object();
    String s = String.valueOf(o);
    if (Verifier.nondetInt() == 3) {
      assert false;
    }
  }
}
