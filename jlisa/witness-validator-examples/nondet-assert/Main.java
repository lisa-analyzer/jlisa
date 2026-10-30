import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    int x = Verifier.nondetInt();
    if (x == 0) {
      assert false;
    }
  }
}
