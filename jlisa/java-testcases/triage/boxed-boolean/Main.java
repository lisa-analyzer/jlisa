import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Boolean a = Verifier.nondetBoolean();
    Boolean b = Verifier.nondetBoolean();
    int x = a ? 1 : 0;
    if (a && !b && x == 1) {
      assert false;
    }
  }
}
